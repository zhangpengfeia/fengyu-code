import hashlib
import json
from pathlib import Path

from langchain_core.messages import HumanMessage
from sqlalchemy import select

from src.backends.models import EvaluationJob
from src.schemas.api import AttemptData, PromptSnapshot
from src.utils.errors import AppError


class MockPromptRepository:
    def __init__(self, settings, catalog, data):
        self.settings, self.catalog, self.data = settings, catalog, data
        folder = Path(__file__).resolve().parents[2] / "config/prompts"
        self.templates = {
            settings.base_prompt_key: (folder / "companion.base.md").read_text(),
            settings.chat_prompt_key: (folder / "companion.chat.md").read_text(),
            settings.evaluation_prompt_key: (folder / "companion.evaluation.md").read_text(),
        }
        for game in catalog.games.values():
            self.templates[game.prompt_key] = f"【Mock 规则】{game.name}\n{data['rule_text']}"

    async def get(self, key, version):
        if key not in self.templates or version != "1.0.0":
            raise AppError("RULES_UNAVAILABLE", "Mock 数据中没有此版本规则")
        template = self.templates[key]
        return PromptSnapshot(
            namespace="local-mock",
            key=key,
            version=version,
            template=template,
            digest=hashlib.sha256(template.encode()).hexdigest(),
        )


def json_context(messages, marker):
    for message in reversed(messages):
        if isinstance(message.content, str) and marker in message.content:
            start = message.content.find("{")
            if start >= 0:
                try:
                    return json.JSONDecoder().raw_decode(message.content[start:])[0]
                except ValueError:
                    pass
    return None


class MockModelGateway:
    """确定性示例回复；保留与真实模型相同的服务接口，绝不发出网络请求。"""

    async def answer(self, messages):
        evaluation_facts = json_context(messages, "可信业务数据")
        if evaluation_facts:
            facts = evaluation_facts
            return json.dumps(
                {
                    "performance": f"【Mock 测评】本局{'已通关' if facts['passed'] else '未通关'}，"
                    f"用时 {facts['duration_ms'] // 1000} 秒，操作 {facts['actual_steps']} 步。"
                    "这份评价用于验证数据链路，不是大模型能力评估。",
                    "suggestions": [
                        "先回顾游戏目标，再在训练模式中尝试规划操作。",
                        "留意每一步操作的结果，逐步熟悉规则。",
                    ],
                    "limitations": [
                        "当前使用本地 mock 模型与示例数据，未调用真实大模型。",
                        "未采集操作轨迹，不判断错误次数或能力分数。",
                    ],
                },
                ensure_ascii=False,
            )
        report = json_context(messages, "已授权的本局事实与已有报告")
        if report:
            facts = report["facts"]
            return (
                f"【Mock 联调回复】我正在解释这份已有报告：本局徽章为 {facts['badge']}，"
                f"实际操作 {facts['actual_steps']} 步，用时 {facts['duration_ms'] // 1000} 秒。\n"
                "徽章来自后端示例事实；没有评分细则时，不能推断具体扣分原因。"
                "这次追问没有重新生成测评。"
            )
        rules = json_context(messages, "受控游戏规则")
        question = next((m.content for m in reversed(messages) if isinstance(m, HumanMessage)), "")
        if rules:
            return (
                f"【Mock 联调回复】已读取 {rules['game_code']} 的 {rules['version']} 版示例规则。\n"
                f"{rules['content']}\n你的问题是：{question}\n"
                "配置真实 Nacos 与模型后，将由相同问答流程生成正式回复。"
            )
        return (
            f"【Mock 联调回复】你好，我是你的学习伙伴！我收到了：{question}\n"
            "可以告诉我游戏名称了解示例规则，或完成一局联调测评后追问报告。"
            "当前回复来自后端本地 mock 模型。"
        )


class MockAttemptSource:
    def __init__(self, database, app_id):
        self.database, self.app_id = database, app_id

    async def fetch(self, attempt_id, revision):
        async with self.database.sessions() as session:
            job = await session.scalar(
                select(EvaluationJob).where(
                    EvaluationJob.app_id == self.app_id,
                    EvaluationJob.source_attempt_id == attempt_id,
                    EvaluationJob.source_revision == revision,
                )
            )
            if not job or not job.facts:
                raise AppError("ATTEMPT_NOT_READY", "Mock 本局数据尚未就绪")
            return AttemptData.model_validate(job.facts)
