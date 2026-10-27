import logging
import time

from src.agents.evaluation_agent import EvaluationAgent
from src.backends.evaluation_repository import EvaluationRepository
from src.backends.models import EvaluationJob
from src.config.game_catalog import Catalog
from src.config.nacos.prompts import NacosPromptRepository
from src.config.settings import Settings
from src.integrations.attempt_source import AttemptSource
from src.schemas.api import AttemptData
from src.services.prompt_resolver import PromptResolver
from src.utils.errors import AppError
from src.utils.model_gateway import ModelGateway

logger = logging.getLogger(__name__)


class EvaluationService:
    def __init__(
        self,
        settings: Settings,
        repository: EvaluationRepository,
        catalog: Catalog,
        source: AttemptSource,
        prompts: NacosPromptRepository,
        model: ModelGateway,
        resolver: PromptResolver | None = None,
    ):
        self.settings, self.repository, self.catalog = settings, repository, catalog
        self.source, self.prompts, self.model = source, prompts, model
        self.resolver = resolver or PromptResolver(settings, catalog, prompts)

    async def run_once(self) -> bool:
        if self.settings.demo_responses:
            return False
        job = await self.repository.claim_job(self.settings.job_lease_seconds)
        if not job:
            return False
        try:
            await self.evaluate(job)
        except AppError as exc:
            await self.fail_or_retry(job, exc.code)
        except Exception:
            # 不记录学生事实、Prompt 正文或上游异常字符串。
            logger.error("evaluation_failed job_id=%s code=UNEXPECTED_ERROR", job.id)
            await self.fail_or_retry(job, "UNEXPECTED_ERROR")
        return True

    async def evaluate(self, job: EvaluationJob) -> None:
        facts = (
            AttemptData.model_validate(job.facts)
            if job.facts
            else await self.source.fetch(job.source_attempt_id, job.source_revision)
        )
        if facts.mode == "TRAINING":
            await self.repository.update_job(
                job,
                status="SKIPPED_TRAINING",
                lease_until=0,
                facts=None,
                student_id=None,
                school_id=None,
                game_code=None,
                ended_at=None,
                error_code=None,
            )
            return
        self.catalog.get(facts.game_code)
        # 固定事实快照后模型重试不再读取被修改的数据；来源修订必须有新 revision。
        job.facts = facts.model_dump(mode="json")
        await self.repository.update_job(
            job,
            facts=job.facts,
            student_id=facts.student_id,
            school_id=facts.school_id,
            game_code=facts.game_code,
            ended_at=facts.ended_at.timestamp(),
        )
        bundle = await self.resolver.resolve_evaluation_context(facts, job.prompt_snapshots)
        snapshots = bundle.to_snapshots()
        if job.prompt_snapshots != snapshots:
            job.prompt_snapshots = snapshots
            await self.repository.update_job(job, prompt_snapshots=job.prompt_snapshots)
        text = await EvaluationAgent(self.model).evaluate(facts, bundle)
        await self.repository.update_job(
            job,
            status="COMPLETED",
            lease_until=0,
            error_code=None,
            report={
                "facts": job.facts,
                "evaluation": text.model_dump(),
                "generation_mode": "MOCK" if self.settings.mock_mode else "MODEL",
                "model": self.settings.runtime_model_name,
            },
        )

    async def fail_or_retry(self, job: EvaluationJob, code: str) -> None:
        permanent = code in {"INVALID_ATTEMPT_DATA", "ATTEMPT_REVISION_MISMATCH", "GAME_NOT_FOUND"}
        if not permanent and job.attempts < self.settings.job_max_attempts:
            await self.repository.update_job(
                job,
                status="WAITING_DATA" if code == "ATTEMPT_NOT_READY" else "RETRY_WAIT",
                error_code=code,
                lease_until=0,
                available_at=time.time() + min(300, 2**job.attempts),
            )
            return
        # 仅模型调用/输出失败可基于已验证事实降级；规则缺失时不生成评价。
        if (
            job.facts
            and job.prompt_snapshots
            and code in {"MODEL_UNAVAILABLE", "EMPTY_MODEL_RESPONSE", "INVALID_EVALUATION_OUTPUT"}
        ):
            await self.repository.update_job(
                job,
                status="COMPLETED_WITH_FALLBACK",
                lease_until=0,
                error_code=code,
                report={
                    "facts": job.facts,
                    "generation_mode": "TEMPLATE",
                    "model": None,
                    "evaluation": {
                        "performance": "本局结果已记录，请查看事实卡片中的通关与徽章信息。",
                        "suggestions": ["可以回顾游戏规则，在训练模式中巩固操作方法。"],
                        "limitations": ["自动分析暂时不可用，当前展示固定模板建议。"],
                    },
                },
            )
        else:
            await self.repository.update_job(job, status="FAILED", lease_until=0, error_code=code)
