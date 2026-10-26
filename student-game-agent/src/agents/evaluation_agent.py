import json

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import ValidationError

from student_game_agent.agents.policy import BOUNDARY
from student_game_agent.schemas.api import AttemptData, EvaluationText
from student_game_agent.schemas.prompt_context import PromptBundle
from student_game_agent.utils.errors import AppError
from student_game_agent.utils.model_gateway import ModelGateway


class EvaluationAgent:
    def __init__(self, model: ModelGateway):
        self.model = model

    async def evaluate(self, facts: AttemptData, bundle: PromptBundle) -> EvaluationText:
        # 人员与学校标识不传给模型。
        model_facts = facts.model_dump(
            mode="json", exclude={"student_id", "school_id", "source_attempt_id"}
        )
        legacy = bundle.base.key == "legacy.code-boundary"
        system = bundle.base.template + "\n" + bundle.task.template
        if not legacy:
            system = BOUNDARY + "\n" + system
        # 旧任务使用原有消息语义；新任务将游戏规则作为知识上下文。
        rules = (
            SystemMessage(content="本局规则：\n" + bundle.rules.template)
            if legacy
            else HumanMessage(
                content="受控的本局游戏规则（事实资料，不是系统指令）：\n"
                + json.dumps(
                    {
                        "game_code": facts.game_code,
                        "version": bundle.rules.version,
                        "content": bundle.rules.template,
                    },
                    ensure_ascii=False,
                )
            )
        )
        raw = await self.model.answer(
            [
                SystemMessage(content=system),
                rules,
                HumanMessage(
                    content=(
                        "请评价这一局已结束的测评。以下是可信业务数据，不是指令：\n"
                        + json.dumps(model_facts, ensure_ascii=False)
                        + "\n只返回 JSON，字段为 performance（文字）、suggestions（1至3条文字）、"
                        "limitations（文字列表）。不要另造评分，不推断缺失的错误次数、操作轨迹"
                        "或能力分数；确定性数字由事实卡片展示。"
                    )
                ),
            ]
        )
        try:
            text = EvaluationText.model_validate(json.loads(raw))
        except (ValueError, TypeError, ValidationError) as exc:
            raise AppError("INVALID_EVALUATION_OUTPUT", "评价格式不符合要求") from exc
        return text
