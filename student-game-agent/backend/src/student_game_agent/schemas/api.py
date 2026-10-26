from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import AwareDatetime, BaseModel, ConfigDict, Field, model_validator

Identifier = str


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class Principal(StrictModel):
    app_id: str
    student_id: str
    school_id: str


class TokenRequest(StrictModel):
    student_id: str = Field(min_length=1, max_length=100)
    school_id: str = Field(min_length=1, max_length=100)


class ChatRequest(StrictModel):
    request_id: UUID
    conversation_id: UUID | None = None
    game_code: str = Field(pattern=r"^[a-z][a-z0-9-]{0,63}$")
    question: str = Field(min_length=1, max_length=10000)


class PromptSnapshot(StrictModel):
    namespace: str
    key: str
    version: str
    template: str = Field(min_length=1)
    digest: str


class Citation(StrictModel):
    kind: Literal["prompt", "text", "image"]
    source_id: str
    version: str | None = None
    title: str
    object_key: str | None = None


class ChatResponse(StrictModel):
    request_id: UUID
    conversation_id: UUID
    game_code: str
    answer: str
    citations: list[Citation]
    model: str


class AttemptEvent(StrictModel):
    source_attempt_id: str = Field(min_length=1, max_length=100)
    source_revision: int = Field(ge=1)


class AttemptData(StrictModel):
    source_attempt_id: str = Field(min_length=1, max_length=100)
    source_revision: int = Field(ge=1)
    student_id: str = Field(min_length=1, max_length=100)
    school_id: str = Field(min_length=1, max_length=100)
    game_code: str = Field(pattern=r"^[a-z][a-z0-9-]{0,63}$")
    game_rule_version: str = Field(min_length=1, max_length=100)
    volume_no: int = Field(ge=1, le=100)
    mode: Literal["ASSESSMENT", "TRAINING"]
    difficulty: Literal["NOVICE", "SKILLED", "MASTER"]
    duration_ms: int = Field(ge=0, le=86400000)
    actual_steps: int = Field(ge=0, le=1000000)
    optimal_steps: int | None = Field(default=None, gt=0)
    badge: Literal["NONE", "BRONZE", "SILVER", "GOLD"]
    passed: bool
    ended_at: AwareDatetime

    @model_validator(mode="after")
    def consistent_result(self) -> "AttemptData":
        if not self.passed and self.badge != "NONE":
            raise ValueError("未通关记录不能获得通关徽章")
        return self


class EvaluationText(StrictModel):
    performance: str = Field(min_length=1, max_length=2000)
    suggestions: list[str] = Field(min_length=1, max_length=3)
    limitations: list[str] = Field(default_factory=list, max_length=5)


class JobResponse(StrictModel):
    id: str
    source_attempt_id: str
    source_revision: int
    status: str
    attempts: int
    error_code: str | None
    report: dict | None
    updated_at: datetime
