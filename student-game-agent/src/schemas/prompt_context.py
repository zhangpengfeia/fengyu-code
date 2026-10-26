from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, model_validator

from src.schemas.api import PromptSnapshot

ChatScene = Literal["GENERAL_CHAT", "GAME_QA", "REPORT_QA"]
PromptScene = Literal["GENERAL_CHAT", "GAME_QA", "REPORT_QA", "POSTGAME_EVALUATION"]


class ArchivedPolicySnapshot(PromptSnapshot):
    """归档代码策略时保留原始空白，与历史注入的指令逐字一致。"""

    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=False)


class PromptBundle(BaseModel):
    """一次请求固定的模板组合；顺序为通用、任务、可选游戏规则。"""

    model_config = ConfigDict(extra="forbid", frozen=True)

    scene: PromptScene
    base: PromptSnapshot
    task: PromptSnapshot
    rules: PromptSnapshot | None = None

    @model_validator(mode="after")
    def validate_composition(self) -> Self:
        if self.scene == "GENERAL_CHAT" and self.rules is not None:
            raise ValueError("普通聊天不得携带游戏规则")
        if self.scene != "GENERAL_CHAT" and self.rules is None:
            raise ValueError("游戏问答、报告问答和测评必须绑定游戏规则")
        return self

    @property
    def snapshots(self) -> list[PromptSnapshot]:
        values = [self.base, self.task]
        if self.rules is not None:
            values.append(self.rules)
        return values

    def to_snapshots(self) -> list[dict]:
        return [snapshot.model_dump(mode="json") for snapshot in self.snapshots]

    @classmethod
    def from_snapshots(cls, scene: PromptScene, snapshots: list[dict]) -> Self:
        required = 2 if scene == "GENERAL_CHAT" else 3
        if len(snapshots) != required:
            raise ValueError(f"{scene} 配置快照必须包含 {required} 项")
        if any(not isinstance(value, dict) for value in snapshots):
            raise ValueError("模板快照的每一项必须是对象")
        values = [
            (
                ArchivedPolicySnapshot.model_validate(value)
                if value.get("key") == "legacy.code-boundary"
                else PromptSnapshot.model_validate(value)
            )
            for value in snapshots
        ]
        return cls(
            scene=scene,
            base=values[0],
            task=values[1],
            rules=values[2] if required == 3 else None,
        )
