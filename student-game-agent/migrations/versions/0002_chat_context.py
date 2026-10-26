"""Optional game context, report references and chronological report lookup."""

from datetime import datetime

import sqlalchemy as sa
from alembic import context, op

revision = "0002"
down_revision = "0001"
branch_labels = None
depends_on = None


def upgrade():
    with op.batch_alter_table("conversation") as batch:
        batch.alter_column("game_code", existing_type=sa.String(64), nullable=True)
        batch.add_column(sa.Column("rule_version", sa.String(100), nullable=True))
        batch.add_column(sa.Column("evaluation_id", sa.String(36), nullable=True))
        batch.add_column(
            sa.Column("context_scene", sa.String(32), nullable=False, server_default="GENERAL_CHAT")
        )
    with op.batch_alter_table("evaluation_job") as batch:
        batch.add_column(sa.Column("game_code", sa.String(64), nullable=True))
        batch.add_column(sa.Column("ended_at", sa.Float(), nullable=True))
        batch.create_index("ix_evaluation_job_ended_at", ["ended_at"])
    if context.is_offline_mode():
        return
    connection = op.get_bind()
    jobs = sa.table(
        "evaluation_job",
        sa.column("id", sa.String),
        sa.column("facts", sa.JSON),
        sa.column("game_code", sa.String),
        sa.column("ended_at", sa.Float),
    )
    for row in connection.execute(sa.select(jobs.c.id, jobs.c.facts)).mappings():
        facts = row["facts"]
        if not isinstance(facts, dict) or not facts.get("game_code"):
            continue
        try:
            ended_at = datetime.fromisoformat(facts["ended_at"])
            if ended_at.tzinfo is None:
                continue
        except (KeyError, ValueError, TypeError):
            continue
        connection.execute(
            jobs.update()
            .where(jobs.c.id == row["id"])
            .values(game_code=facts["game_code"], ended_at=ended_at.timestamp())
        )
    conversations = sa.table(
        "conversation",
        sa.column("id", sa.String),
        sa.column("game_code", sa.String),
        sa.column("context_scene", sa.String),
        sa.column("rule_version", sa.String),
    )
    connection.execute(
        conversations.update()
        .where(conversations.c.game_code.is_not(None))
        .values(context_scene="GAME_QA")
    )
    runs = sa.table(
        "chat_run",
        sa.column("id", sa.String),
        sa.column("conversation_id", sa.String),
        sa.column("response", sa.JSON),
        sa.column("prompt_snapshots", sa.JSON),
        sa.column("created_at", sa.Float),
    )
    for row in connection.execute(sa.select(runs).order_by(runs.c.created_at)).mappings():
        response = row["response"]
        if not isinstance(response, dict) or not response.get("game_code"):
            continue
        response = dict(response)
        version = next(
            (
                p.get("version")
                for p in row["prompt_snapshots"] or []
                if p.get("key") == "game.rules." + response["game_code"]
            ),
            None,
        )
        response.setdefault("scene", "GAME_QA")
        response.setdefault("game_rule_version", version)
        connection.execute(runs.update().where(runs.c.id == row["id"]).values(response=response))
        if version:
            connection.execute(
                conversations.update()
                .where(conversations.c.id == row["conversation_id"])
                .values(rule_version=version)
            )


def downgrade():
    if not context.is_offline_mode():
        table = sa.table("conversation", sa.column("game_code"))
        if op.get_bind().scalar(sa.select(sa.func.count()).where(table.c.game_code.is_(None))):
            raise RuntimeError(
                "存在未绑定游戏的自由对话，无法无损降级到 0001；请保留数据并回滚应用"
            )
    with op.batch_alter_table("evaluation_job") as batch:
        batch.drop_index("ix_evaluation_job_ended_at")
        batch.drop_column("ended_at")
        batch.drop_column("game_code")
    with op.batch_alter_table("conversation") as batch:
        batch.drop_column("context_scene")
        batch.drop_column("evaluation_id")
        batch.drop_column("rule_version")
        batch.alter_column("game_code", existing_type=sa.String(64), nullable=False)
