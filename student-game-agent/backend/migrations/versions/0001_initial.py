"""Initial durable chat and evaluation schema."""

import sqlalchemy as sa
from alembic import op

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "conversation",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("app_id", sa.String(100), nullable=False),
        sa.Column("student_id", sa.String(100), nullable=False),
        sa.Column("school_id", sa.String(100), nullable=False),
        sa.Column("game_code", sa.String(64), nullable=False),
        sa.Column("busy_until", sa.Float(), nullable=False),
        sa.Column("busy_token", sa.String(36), nullable=False),
    )
    op.create_table(
        "chat_run",
        sa.Column("id", sa.String(64), primary_key=True),
        sa.Column("conversation_id", sa.String(36), nullable=False),
        sa.Column("request_id", sa.String(36), nullable=False),
        sa.Column("payload_hash", sa.String(64), nullable=False),
        sa.Column("question", sa.Text(), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("execution_token", sa.String(36), nullable=False),
        sa.Column("lease_until", sa.Float(), nullable=False),
        sa.Column("created_at", sa.Float(), nullable=False),
        sa.Column("response", sa.JSON(), nullable=True),
        sa.Column("prompt_snapshots", sa.JSON(), nullable=True),
    )
    op.create_index("ix_chat_run_conversation_id", "chat_run", ["conversation_id"])
    op.create_table(
        "evaluation_job",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("app_id", sa.String(100), nullable=False),
        sa.Column("source_attempt_id", sa.String(100), nullable=False),
        sa.Column("source_revision", sa.Integer(), nullable=False),
        sa.Column("student_id", sa.String(100), nullable=True),
        sa.Column("school_id", sa.String(100), nullable=True),
        sa.Column("status", sa.String(32), nullable=False),
        sa.Column("attempts", sa.Integer(), nullable=False),
        sa.Column("available_at", sa.Float(), nullable=False),
        sa.Column("lease_until", sa.Float(), nullable=False),
        sa.Column("execution_token", sa.String(36), nullable=False),
        sa.Column("error_code", sa.String(100), nullable=True),
        sa.Column("facts", sa.JSON(), nullable=True),
        sa.Column("prompt_snapshots", sa.JSON(), nullable=True),
        sa.Column("report", sa.JSON(), nullable=True),
        sa.Column("updated_at", sa.Float(), nullable=False),
        sa.UniqueConstraint(
            "app_id", "source_attempt_id", "source_revision", name="uq_attempt_revision"
        ),
    )
    op.create_index("ix_evaluation_job_status", "evaluation_job", ["status"])
    op.create_index("ix_evaluation_job_available_at", "evaluation_job", ["available_at"])


def downgrade():
    op.drop_table("evaluation_job")
    op.drop_table("chat_run")
    op.drop_table("conversation")
