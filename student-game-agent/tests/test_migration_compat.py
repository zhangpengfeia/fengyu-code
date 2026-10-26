import asyncio
from datetime import UTC, datetime
from pathlib import Path
from uuid import uuid4

import httpx
import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from conftest import Upstream, student_headers
from sqlalchemy.ext.asyncio import create_async_engine

from src.app.container import Container
from src.app.main import create_app
from src.backends.chat_repository import digest
from src.config.settings import Settings

BACKEND_ROOT = Path(__file__).resolve().parents[1]


def migration_settings(tmp_path):
    settings = Settings(
        _env_file=None,
        environment="test",
        service_api_key="s" * 40,
        jwt_secret="j" * 40,
        database_url=f"sqlite+aiosqlite:///{tmp_path / 'legacy.db'}",
        nacos_url="http://nacos.test",
        nacos_auth_enabled=False,
        llm_base_url="http://llm.test/v1",
        llm_api_key="test-model-key",
        llm_model="test-model",
    )
    migration = Config(str(BACKEND_ROOT / "alembic.ini"))
    migration.attributes["settings"] = settings
    return settings, migration


def legacy_tables():
    # 描述旧版结构，不能使用包含新列的当前 ORM 来写 0001 数据库。
    conversations = sa.table(
        "conversation",
        sa.column("id", sa.String),
        sa.column("app_id", sa.String),
        sa.column("student_id", sa.String),
        sa.column("school_id", sa.String),
        sa.column("game_code", sa.String),
        sa.column("busy_until", sa.Float),
        sa.column("busy_token", sa.String),
    )
    runs = sa.table(
        "chat_run",
        sa.column("id", sa.String),
        sa.column("conversation_id", sa.String),
        sa.column("request_id", sa.String),
        sa.column("payload_hash", sa.String),
        sa.column("question", sa.Text),
        sa.column("status", sa.String),
        sa.column("execution_token", sa.String),
        sa.column("lease_until", sa.Float),
        sa.column("created_at", sa.Float),
        sa.column("response", sa.JSON),
        sa.column("prompt_snapshots", sa.JSON),
    )
    jobs = sa.table(
        "evaluation_job",
        sa.column("id", sa.String),
        sa.column("app_id", sa.String),
        sa.column("source_attempt_id", sa.String),
        sa.column("source_revision", sa.Integer),
        sa.column("student_id", sa.String),
        sa.column("school_id", sa.String),
        sa.column("status", sa.String),
        sa.column("attempts", sa.Integer),
        sa.column("available_at", sa.Float),
        sa.column("lease_until", sa.Float),
        sa.column("execution_token", sa.String),
        sa.column("error_code", sa.String),
        sa.column("facts", sa.JSON),
        sa.column("prompt_snapshots", sa.JSON),
        sa.column("report", sa.JSON),
        sa.column("updated_at", sa.Float),
    )
    return conversations, runs, jobs


async def seed_legacy_database(settings, migration):
    await asyncio.to_thread(command.upgrade, migration, "0001")
    conversation_id, request_id, job_id = (str(uuid4()) for _ in range(3))
    principal = {
        "app_id": settings.integration_app_id,
        "student_id": "student-1",
        "school_id": "school-1",
    }
    request = {
        "request_id": request_id,
        "conversation_id": conversation_id,
        "game_code": "eight-pieces",
        "question": "这个游戏怎么玩？",
    }
    response = {
        "request_id": request_id,
        "conversation_id": conversation_id,
        "game_code": "eight-pieces",
        "answer": "迁移前已保存的回答。",
        "citations": [],
        "model": "legacy-model",
    }
    snapshot = {
        "namespace": "student-game-agent",
        "key": "game.rules.eight-pieces",
        "version": "historical-v1",
        "template": "迁移前的规则文本。",
        "digest": "legacy-rule-digest",
    }
    old_system = {**snapshot, "key": "companion.system", "version": "1.0.0"}
    old_task = {**snapshot, "key": "companion.evaluation", "version": "1.0.0"}
    facts = {
        **Upstream().facts,
        "game_rule_version": "historical-v1",
        "ended_at": "2026-10-26T10:00:00+08:00",
    }
    report = {"facts": facts, "evaluation": {"performance": "旧评价。"}}
    conversations, runs, jobs = legacy_tables()
    engine = create_async_engine(settings.database_url)
    try:
        async with engine.begin() as connection:
            await connection.execute(
                conversations.insert().values(
                    id=conversation_id,
                    **principal,
                    game_code="eight-pieces",
                    busy_until=0,
                    busy_token="",
                )
            )
            await connection.execute(
                runs.insert().values(
                    id=digest([principal, request_id]),
                    conversation_id=conversation_id,
                    request_id=request_id,
                    payload_hash=digest(request),
                    question=request["question"],
                    status="COMPLETED",
                    execution_token=str(uuid4()),
                    lease_until=0,
                    created_at=1,
                    response=response,
                    prompt_snapshots=[old_system, snapshot],
                )
            )
            await connection.execute(
                jobs.insert().values(
                    id=job_id,
                    **principal,
                    source_attempt_id=facts["source_attempt_id"],
                    source_revision=1,
                    status="COMPLETED",
                    attempts=1,
                    available_at=0,
                    lease_until=0,
                    execution_token=str(uuid4()),
                    error_code=None,
                    facts=facts,
                    prompt_snapshots=[snapshot, old_task],
                    report=report,
                    updated_at=2,
                )
            )
    finally:
        await engine.dispose()
    return {"conversation_id": conversation_id, "job_id": job_id, "request": request}


def test_upgrade_preserves_records_and_backfills_context_and_report_lookup(tmp_path):
    async def scenario():
        settings, migration = migration_settings(tmp_path)
        seeded = await seed_legacy_database(settings, migration)
        await asyncio.to_thread(command.upgrade, migration, "head")
        engine = create_async_engine(settings.database_url)
        try:
            async with engine.connect() as connection:
                conversation = (
                    (await connection.execute(sa.text("SELECT * FROM conversation")))
                    .mappings()
                    .one()
                )
                assert conversation["id"] == seeded["conversation_id"]
                assert conversation["context_scene"] == "GAME_QA"
                assert conversation["rule_version"] == "historical-v1"
                assert conversation["game_code"] == "eight-pieces"
                _, runs, jobs = legacy_tables()
                run = (await connection.execute(sa.select(runs))).mappings().one()
                assert run["response"]["answer"] == "迁移前已保存的回答。"
                assert run["response"]["scene"] == "GAME_QA"
                assert run["response"]["game_rule_version"] == "historical-v1"
                assert len(run["prompt_snapshots"]) == 2
                old_job = (await connection.execute(sa.select(jobs))).mappings().one()
                assert old_job["id"] == seeded["job_id"]
                assert old_job["report"]["evaluation"]["performance"] == "旧评价。"
                assert len(old_job["prompt_snapshots"]) == 2
                lookup = (
                    (
                        await connection.execute(
                            sa.text("SELECT game_code, ended_at FROM evaluation_job")
                        )
                    )
                    .mappings()
                    .one()
                )
                assert lookup["game_code"] == "eight-pieces"
                assert lookup["ended_at"] == datetime(2026, 10, 26, 2, tzinfo=UTC).timestamp()
                assert (
                    await connection.scalar(sa.text("SELECT version_num FROM alembic_version"))
                    == "0002"
                )
        finally:
            await engine.dispose()

    asyncio.run(scenario())


def test_legacy_request_replays_saved_answer_after_upgrade_without_model_call(tmp_path):
    async def scenario():
        settings, migration = migration_settings(tmp_path)
        seeded = await seed_legacy_database(settings, migration)
        await asyncio.to_thread(command.upgrade, migration, "head")
        upstream = Upstream()
        container = Container(
            settings, httpx.AsyncClient(transport=httpx.MockTransport(upstream.handle))
        )
        app = create_app(container=container)
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(
                transport=httpx.ASGITransport(app=app), base_url="http://agent.test"
            ) as client:
                headers = await student_headers(client)
                result = await client.post("/api/v1/chat", headers=headers, json=seeded["request"])
                assert result.status_code == 200, result.text
                assert result.json()["answer"] == "迁移前已保存的回答。"
                assert result.json()["scene"] == "GAME_QA"
                assert result.json()["game_rule_version"] == "historical-v1"
                assert result.json()["model"] == "legacy-model"
                assert not upstream.calls

    asyncio.run(scenario())


def test_downgrade_rejects_free_chat_without_losing_rows_or_revision(tmp_path):
    async def scenario():
        settings, migration = migration_settings(tmp_path)
        seeded = await seed_legacy_database(settings, migration)
        await asyncio.to_thread(command.upgrade, migration, "head")
        free_conversation = str(uuid4())
        engine = create_async_engine(settings.database_url)
        try:
            async with engine.begin() as connection:
                await connection.execute(
                    sa.text(
                        "INSERT INTO conversation "
                        "(id, app_id, student_id, school_id, game_code, context_scene, "
                        "busy_until, busy_token) VALUES "
                        "(:id, :app_id, 'student-1', 'school-1', NULL, 'GENERAL_CHAT', 0, '')"
                    ),
                    {"id": free_conversation, "app_id": settings.integration_app_id},
                )
            with pytest.raises(RuntimeError, match="无法无损降级"):
                await asyncio.to_thread(command.downgrade, migration, "0001")
            async with engine.connect() as connection:
                rows = (
                    (await connection.execute(sa.text("SELECT id, game_code FROM conversation")))
                    .mappings()
                    .all()
                )
                assert {row["id"] for row in rows} == {seeded["conversation_id"], free_conversation}
                assert (
                    next(row for row in rows if row["id"] == free_conversation)["game_code"] is None
                )
                assert await connection.scalar(sa.text("SELECT COUNT(*) FROM chat_run")) == 1
                assert await connection.scalar(sa.text("SELECT COUNT(*) FROM evaluation_job")) == 1
                assert (
                    await connection.scalar(sa.text("SELECT version_num FROM alembic_version"))
                    == "0002"
                )
        finally:
            await engine.dispose()

    asyncio.run(scenario())


def test_readiness_rejects_old_schema_before_upgrade(tmp_path):
    async def scenario():
        settings, migration = migration_settings(tmp_path)
        await asyncio.to_thread(command.upgrade, migration, "0001")
        upstream = Upstream()
        container = Container(
            settings, httpx.AsyncClient(transport=httpx.MockTransport(upstream.handle))
        )
        app = create_app(container=container)
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(
                transport=httpx.ASGITransport(app=app), base_url="http://agent.test"
            ) as client:
                assert (await client.get("/health/live")).status_code == 200
                ready = await client.get("/health/ready")
                assert ready.status_code == 503
                assert ready.json()["error"]["code"] == "DATABASE_MIGRATION_REQUIRED"
                assert not upstream.calls

    asyncio.run(scenario())
