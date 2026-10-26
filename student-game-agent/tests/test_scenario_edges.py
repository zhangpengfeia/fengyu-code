import asyncio
import json
import time

from conftest import student_headers
from sqlalchemy import select, update
from test_chat_context import message, seed_report

from student_game_agent.backends.models import EvaluationJob
from student_game_agent.schemas.api import AttemptData


def test_rule_question_with_first_person_does_not_require_a_report(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("我怎么获得金徽章？", game_code="eight-pieces"),
            )
            assert response.status_code == 200
            assert response.json()["scene"] == "GAME_QA"
            assert len(upstream.model_requests) == 1

    asyncio.run(scenario())


def test_report_followup_with_page_game_keeps_report_and_snapshot(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            facts = AttemptData.model_validate(upstream.facts)
            bundle = await container.prompt_resolver.resolve_evaluation_context(facts)
            job_id = await seed_report(container, upstream, prompt_snapshots=bundle.to_snapshots())
            headers = await student_headers(client)
            result = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("我这局为什么没获得金徽章？", evaluation_id=job_id),
            )
            assert result.status_code == 200
            upstream.calls.clear()
            upstream.nacos_code = 404
            followup = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message(
                    "为什么？",
                    conversation_id=result.json()["conversation_id"],
                    game_code="eight-pieces",
                ),
            )
            assert followup.status_code == 200
            assert followup.json()["scene"] == "REPORT_QA"
            assert followup.json()["evaluation_id"] == job_id
            assert not [call for call in upstream.calls if call.url.host == "nacos.test"]
            assert any(m["role"] == "assistant" for m in upstream.model_requests[-1]["messages"])

    asyncio.run(scenario())


def test_ambiguous_game_names_do_not_load_any_rules(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            response = await client.post(
                "/api/v1/chat",
                headers=await student_headers(client),
                json=message("八子归位和鱼的游戏怎么操作？"),
            )
            assert response.status_code == 200
            assert response.json()["needs_clarification"]
            assert not upstream.calls

    asyncio.run(scenario())


def test_common_prewarm_does_not_load_games(runtime):
    async def scenario():
        async with runtime(prompt_prewarm=True) as (client, container, upstream):
            assert {call.url.params["promptKey"] for call in upstream.calls} == {
                "companion.base",
                "companion.chat",
            }
            response = await client.post(
                "/api/v1/chat", headers=await student_headers(client), json=message("你好")
            )
            assert response.status_code == 200
            assert len([call for call in upstream.calls if call.url.host == "nacos.test"]) == 2

    asyncio.run(scenario())


def test_worker_retry_reuses_all_prompt_snapshots_and_original_facts(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            job = await container.evaluation_repository.enqueue(
                container.settings.integration_app_id, "attempt-1", 1
            )
            upstream.model_status = 503
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                stored = await session.get(EvaluationJob, job.id)
                assert stored.status == "RETRY_WAIT" and len(stored.prompt_snapshots) == 3
                snapshots = stored.prompt_snapshots
            upstream.model_status = 200
            upstream.model_answer = json.dumps(
                {"performance": "本局已完成。", "suggestions": ["回顾规则。"]}
            )
            upstream.nacos_code = 404
            upstream.facts["badge"] = "GOLD"
            upstream.calls.clear()
            async with container.database.sessions.begin() as session:
                await session.execute(
                    update(EvaluationJob)
                    .where(EvaluationJob.id == job.id)
                    .values(available_at=time.time() - 1)
                )
            await container.evaluations.run_once()
            async with container.database.sessions() as session:
                stored = await session.scalar(
                    select(EvaluationJob).where(EvaluationJob.id == job.id)
                )
                assert stored.status == "COMPLETED"
                assert stored.report["facts"]["badge"] == "BRONZE"
                assert stored.prompt_snapshots == snapshots
            assert not [
                call for call in upstream.calls if call.url.host in {"nacos.test", "attempts.test"}
            ]

    asyncio.run(scenario())


def test_report_followup_keeps_selected_old_round_until_latest_is_requested(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            old_id = await seed_report(
                container,
                upstream,
                facts={"source_attempt_id": "older", "ended_at": "2026-10-25T02:00:00Z"},
            )
            new_id = await seed_report(container, upstream, facts={"source_attempt_id": "newer"})
            headers = await student_headers(client)
            result = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("解释这份报告。", evaluation_id=old_id),
            )
            cid = result.json()["conversation_id"]
            follow = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("那我这局的步数是多少？", conversation_id=cid),
            )
            assert follow.status_code == 200
            assert follow.json()["evaluation_id"] == old_id
            newest = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("我的最新测评报告怎么样？", conversation_id=cid),
            )
            assert newest.status_code == 200
            assert newest.json()["evaluation_id"] == new_id

    asyncio.run(scenario())


def test_current_rules_after_old_report_use_current_catalog_version(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            job_id = await seed_report(container, upstream, facts={"game_rule_version": "old-v1"})
            headers = await student_headers(client)
            result = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("解释这份报告。", evaluation_id=job_id),
            )
            response = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message(
                    "介绍八子归位现在的规则。", conversation_id=result.json()["conversation_id"]
                ),
            )
            assert response.status_code == 200
            assert response.json()["scene"] == "GAME_QA"
            assert response.json()["game_rule_version"] == "1.0.0"
            assert not any(
                m["role"] == "assistant" for m in upstream.model_requests[-1]["messages"]
            )

    asyncio.run(scenario())


def test_learning_discussion_does_not_query_game_reports(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            await seed_report(container, upstream)
            response = await client.post(
                "/api/v1/chat",
                headers=await student_headers(client),
                json=message("这次考试成绩不理想，怎么调整心态？"),
            )
            assert response.status_code == 200
            assert response.json()["scene"] == "GENERAL_CHAT"
            assert response.json()["evaluation_id"] is None
            assert response.json()["game_code"] is None
            sent = json.dumps(upstream.model_requests, ensure_ascii=False)
            assert "BRONZE" not in sent
            assert not [
                call
                for call in upstream.calls
                if call.url.host == "nacos.test"
                and call.url.params["promptKey"].startswith("game.rules.")
            ]

    asyncio.run(scenario())
