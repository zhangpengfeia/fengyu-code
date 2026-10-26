import asyncio
import json
import time

from conftest import student_headers
from sqlalchemy import select, update

from student_game_agent.backends.models import EvaluationJob


async def enqueue(client, revision=1):
    response = await client.post(
        "/internal/v1/game-events",
        headers={"X-Integration-Key": "s" * 40},
        json={"source_attempt_id": "attempt-1", "source_revision": revision},
    )
    assert response.status_code == 202, response.text
    return response.json()["job_id"]


def test_end_event_runs_without_question_and_deduplicates(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            upstream.model_answer = json.dumps(
                {
                    "performance": "本局已通关，可以继续巩固操作。",
                    "suggestions": ["先确认目标，再尝试规划。"],
                    "limitations": ["缺少操作轨迹。"],
                },
                ensure_ascii=False,
            )
            job_id = await enqueue(client)
            assert await enqueue(client) == job_id
            assert await container.evaluations.run_once()
            assert not await container.evaluations.run_once()
            headers = await student_headers(client)
            result = await client.get(f"/api/v1/evaluations/{job_id}", headers=headers)
            assert result.status_code == 200
            report = result.json()
            assert report["status"] == "COMPLETED"
            assert report["report"]["facts"]["badge"] == "BRONZE"
            assert report["report"]["generation_mode"] == "MODEL"
            sent = json.dumps(upstream.model_requests, ensure_ascii=False)
            assert "student-1" not in sent and "school-1" not in sent
            other = await student_headers(client, "student-2")
            assert (
                await client.get(f"/api/v1/evaluations/{job_id}", headers=other)
            ).status_code == 404
            assert (await client.get("/api/v1/evaluations", headers=other)).json() == []
            assert len((await client.get("/api/v1/evaluations", headers=headers)).json()) == 1

    asyncio.run(scenario())


def test_delayed_data_survives_worker_restart_and_retries(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            upstream.source_status = 404
            job_id = await enqueue(client)
            assert await container.evaluations.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, job_id)
                assert job.status == "WAITING_DATA"
                assert job.facts is None
            assert not upstream.model_requests
            upstream.source_status = 200
            upstream.model_answer = '{"performance":"本局已完成。","suggestions":["回顾规则。"]}'
            async with container.database.sessions.begin() as session:
                await session.execute(
                    update(EvaluationJob)
                    .where(EvaluationJob.id == job_id)
                    .values(available_at=time.time() - 1)
                )
            # 新建服务对象模拟进程替换，状态完全来自持久库。
            from student_game_agent.services.evaluation_service import EvaluationService

            replacement = EvaluationService(
                container.settings,
                container.evaluation_repository,
                container.catalog,
                container.evaluations.source,
                container.prompts,
                container.model,
            )
            assert await replacement.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, job_id)
                assert job.status == "COMPLETED" and job.attempts == 2

    asyncio.run(scenario())


def test_training_data_is_not_retained_or_sent_to_model(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            upstream.facts["mode"] = "TRAINING"
            job_id = await enqueue(client)
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, job_id)
                assert job.status == "SKIPPED_TRAINING"
                assert job.facts is None and job.student_id is None and job.report is None
            assert not upstream.model_requests
            assert not [c for c in upstream.calls if c.url.host == "nacos.test"]

    asyncio.run(scenario())


def test_invalid_data_and_rules_never_get_fabricated_reports(runtime):
    async def scenario():
        async with runtime(job_max_attempts=1) as (client, container, upstream):
            upstream.facts["duration_ms"] = -1
            job_id = await enqueue(client)
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, job_id)
                assert job.status == "FAILED" and job.error_code == "INVALID_ATTEMPT_DATA"
            upstream.facts["duration_ms"] = 1000
            upstream.facts["source_revision"] = 2
            upstream.nacos_code = 404
            second_id = await enqueue(client, revision=2)
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, second_id)
                assert job.status == "FAILED" and job.report is None
            assert not upstream.model_requests

    asyncio.run(scenario())


def test_model_failure_falls_back_only_with_real_facts_and_rules(runtime):
    async def scenario():
        async with runtime(job_max_attempts=1) as (client, container, upstream):
            upstream.model_status = 503
            job_id = await enqueue(client)
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                job = await session.get(EvaluationJob, job_id)
                assert job.status == "COMPLETED_WITH_FALLBACK"
                assert job.report["facts"]["actual_steps"] == 36
                assert job.report["generation_mode"] == "TEMPLATE"
                assert len(job.prompt_snapshots) == 2

    asyncio.run(scenario())


def test_source_revision_creates_separate_history(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            upstream.model_answer = '{"performance":"本局已完成。","suggestions":["回顾规则。"]}'
            first_id = await enqueue(client)
            await container.evaluations.run_once()
            upstream.facts.update(source_revision=2, actual_steps=30)
            second_id = await enqueue(client, revision=2)
            assert first_id != second_id
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                jobs = (await session.scalars(select(EvaluationJob))).all()
                assert len(jobs) == 2
                assert {j.report["facts"]["actual_steps"] for j in jobs} == {30, 36}

    asyncio.run(scenario())
