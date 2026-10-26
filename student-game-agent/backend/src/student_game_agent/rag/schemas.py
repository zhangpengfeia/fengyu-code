from typing import Literal

from pydantic import Field

from student_game_agent.schemas.api import Principal, StrictModel


class RetrievalQuery(StrictModel):
    text: str
    game_code: str
    rule_version: str
    principal: Principal
    image_object_keys: list[str] = Field(default_factory=list, max_length=5)
    top_k: int = Field(default=5, ge=1, le=10)


class RetrievedDocument(StrictModel):
    id: str
    kind: Literal["text", "image"]
    title: str
    text: str
    # 私有对象键，不接受任意外链；后续由授权下载服务提供图片。
    image_object_key: str | None = None
    source_version: str | None = None
    score: float | None = None
