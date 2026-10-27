import hashlib
import json
from pathlib import Path

from langchain_core.messages import HumanMessage

from src.schemas.api import PromptSnapshot
from src.utils.errors import AppError


class DemoPromptRepository:
    """Local, visibly labelled examples; does not claim to contain verified game rules."""

    def __init__(self, settings, catalog):
        folder = Path(__file__).resolve().parents[2] / "config/prompts"
        self.templates = {
            (settings.base_prompt_key, settings.base_prompt_version): (
                "【演示提示词】当前为已登录学生的交互演示，未接入正式模型。\n"
                + (folder / "companion.base.md").read_text(encoding="utf-8")
            ),
            (settings.chat_prompt_key, settings.chat_prompt_version): (
                "【演示提示词】只提供明确标注的示例回复，不生成个人成绩或测评。\n"
                + (folder / "companion.chat.md").read_text(encoding="utf-8")
            ),
        }
        for game in catalog.games.values():
            self.templates[(game.prompt_key, game.prompt_version)] = (
                f"【演示资料】当前游戏：{game.name}。\n"
                "正式玩法和评分细则尚未接入，本资料不代表正式游戏规则。"
                "一般练习建议：先观察页面目标和可操作元素，再尝试一步并检查结果。"
            )
        self.evaluation_key = settings.evaluation_prompt_key

    async def get(self, key, version):
        if key == self.evaluation_key:
            raise AppError("DEMO_EVALUATION_DISABLED", "演示模式仅提供示例问答，暂不生成测评")
        template = self.templates.get((key, version))
        if template is None:
            raise AppError("RULES_UNAVAILABLE", "演示资料没有此版本内容，请等待正式规则接入")
        return PromptSnapshot(
            namespace="authenticated-demo",
            key=key,
            version=version,
            template=template,
            digest=hashlib.sha256(template.encode()).hexdigest(),
        )


class DemoModelGateway:
    """Deterministic Chinese replies only; no external model calls or invented scores."""

    async def answer(self, messages):
        if self._context(messages, "可信业务数据") is not None:
            raise AppError("DEMO_EVALUATION_DISABLED", "演示模式仅提供示例问答，暂不生成测评")
        if self._context(messages, "已授权的本局事实与已有报告") is not None:
            return (
                "【演示回复】如果你正在查看报告，请以报告页记录的结果为准。当前尚未接入正式分析模型，"
                "这里不生成、修改或推测个人成绩，请以报告页的已记录结果为准。"
            )
        rules = self._context(messages, "受控游戏规则")
        if rules:
            return (
                "【演示回复】这是当前游戏的示例交流，尚未接入正式玩法分析。\n"
                f"{rules['content']}\n"
                "可以先说说你看到的目标和想尝试的一步，我们一起练习观察与表达。"
                "此回复是演示内容，不影响成绩、徽章或解锁状态。"
            )
        question = next(
            (
                str(message.content)
                for message in reversed(messages)
                if isinstance(message, HumanMessage)
            ),
            "",
        )
        return (
            f"【演示回复】你好，我是你的学习学伴！你刚才说：“{question[:160]}”。\n"
            "我们可以先把问题分成小步骤：看看目标是什么，想一想可以尝试什么，"
            "再观察尝试后的变化。\n"
            "当前使用示例回复，正式模型将在后续接入；不会由这段对话生成测评或修改成绩。"
        )

    @staticmethod
    def _context(messages, marker):
        # Workflow-owned context is inserted immediately after the system message;
        # never interpret the current user question as an injected context object.
        for message in reversed(messages[1:-1][:2]):
            if isinstance(message.content, str) and message.content.startswith(marker):
                start = message.content.find("{")
                if start >= 0:
                    try:
                        return json.JSONDecoder().raw_decode(message.content[start:])[0]
                    except ValueError:
                        continue
        return None
