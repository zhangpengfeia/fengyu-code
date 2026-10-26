import time

from sqlalchemy import JSON, Float, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class Conversation(Base):
    __tablename__ = "conversation"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    app_id: Mapped[str] = mapped_column(String(100))
    student_id: Mapped[str] = mapped_column(String(100))
    school_id: Mapped[str] = mapped_column(String(100))
    game_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    rule_version: Mapped[str | None] = mapped_column(String(100), nullable=True)
    evaluation_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    context_scene: Mapped[str] = mapped_column(
        String(32), default="GENERAL_CHAT", server_default="GENERAL_CHAT"
    )
    busy_until: Mapped[float] = mapped_column(Float, default=0)
    busy_token: Mapped[str] = mapped_column(String(36), default="")


class ChatRun(Base):
    __tablename__ = "chat_run"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    conversation_id: Mapped[str] = mapped_column(String(36), index=True)
    request_id: Mapped[str] = mapped_column(String(36))
    payload_hash: Mapped[str] = mapped_column(String(64))
    question: Mapped[str] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(20))
    execution_token: Mapped[str] = mapped_column(String(36))
    lease_until: Mapped[float] = mapped_column(Float)
    created_at: Mapped[float] = mapped_column(Float, default=time.time)
    response: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    prompt_snapshots: Mapped[list | None] = mapped_column(JSON, nullable=True)


class EvaluationJob(Base):
    __tablename__ = "evaluation_job"
    __table_args__ = (
        UniqueConstraint(
            "app_id", "source_attempt_id", "source_revision", name="uq_attempt_revision"
        ),
    )
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    app_id: Mapped[str] = mapped_column(String(100))
    source_attempt_id: Mapped[str] = mapped_column(String(100))
    source_revision: Mapped[int] = mapped_column(Integer)
    student_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    school_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    game_code: Mapped[str | None] = mapped_column(String(64), nullable=True)
    ended_at: Mapped[float | None] = mapped_column(Float, nullable=True, index=True)
    status: Mapped[str] = mapped_column(String(32), default="RECEIVED", index=True)
    attempts: Mapped[int] = mapped_column(Integer, default=0)
    available_at: Mapped[float] = mapped_column(Float, default=time.time, index=True)
    lease_until: Mapped[float] = mapped_column(Float, default=0)
    execution_token: Mapped[str] = mapped_column(String(36), default="")
    error_code: Mapped[str | None] = mapped_column(String(100), nullable=True)
    facts: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    prompt_snapshots: Mapped[list | None] = mapped_column(JSON, nullable=True)
    report: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    updated_at: Mapped[float] = mapped_column(Float, default=time.time)
