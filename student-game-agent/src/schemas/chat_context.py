from typing import Literal

from src.schemas.api import PromptSnapshot, StrictModel


class ChatContext(StrictModel):
    """服务端确认的上下文；与未经验证的页面定位参数分开。"""

    scene: Literal["GENERAL_CHAT", "GAME_QA", "REPORT_QA"]
    game_code: str | None = None
    rule_version: str | None = None
    evaluation_id: str | None = None
    report: dict | None = None
    rule_snapshot: PromptSnapshot | None = None
    direct_answer: str | None = None
    needs_clarification: bool = False
