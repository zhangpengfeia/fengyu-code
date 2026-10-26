import asyncio
import json
from datetime import UTC, datetime
from uuid import uuid4

from conftest import student_headers
from sqlalchemy import func, select

from src.backends.models import EvaluationJob


def message(question, **values):
    return {"request_id": str(uuid4()), "question": question, **values}


def prompt_queries(upstream):
    return [
        (request.url.params["promptKey"], request.url.params["version"])
        for request in upstream.calls
        if request.url.host == "nacos.test"
    ]


async def seed_report(container, upstream, **changes):
    facts = {**upstream.facts, **changes.pop("facts", {})}
    status = changes.pop("status", "COMPLETED")
    job = EvaluationJob(
        id=str(uuid4()),
        app_id=container.settings.integration_app_id,
        source_attempt_id=facts["source_attempt_id"],
        source_revision=facts["source_revision"],
        student_id=facts["student_id"],
        school_id=facts["school_id"],
        game_code=facts["game_code"],
        ended_at=datetime.fromisoformat(facts["ended_at"].replace("Z", "+00:00")).timestamp(),
        status=status,
        facts=facts,
        report={
            "facts": facts,
            "evaluation": {
                "performance": "本局已通关，结果由游戏业务后端确认。",
                "suggestions": ["本局建议先观察目标，再规划下一步。"],
                "limitations": ["本局没有操作轨迹，未判断具体错误次数。"],
            },
            "generation_mode": "MODEL",
            "model": "test-model",
        }
        if status == "COMPLETED"
        else None,
        **changes,
    )
    async with container.database.sessions.begin() as session:
        session.add(job)
    return job.id


def test_general_chat_does_not_require_game_or_load_rules(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            for context in ({}, {"game_code": "eight-pieces"}):
                response = await client.post(
                    "/api/v1/chat", headers=headers, json=message("你好", **context)
                )
                assert response.status_code == 200, response.text
                answer = response.json()
                assert answer["scene"] == "GENERAL_CHAT"
                assert answer["needs_clarification"] is False
                assert answer["game_rule_version"] is None
                assert answer["answer"] == upstream.model_answer
            assert {key for key, version in prompt_queries(upstream)} == {
                "companion.base",
                "companion.chat",
            }
            assert len(upstream.model_requests) == 2

    asyncio.run(scenario())


def test_game_name_in_question_resolves_rules_without_game_code(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat", headers=headers, json=message("八子归位怎么玩？")
            )
            assert response.status_code == 200, response.text
            answer = response.json()
            assert answer["scene"] == "GAME_QA"
            assert answer["game_code"] == "eight-pieces"
            assert answer["game_rule_version"] == "1.0.0"
            assert ("game.rules.eight-pieces", "1.0.0") in prompt_queries(upstream)
            assert len(upstream.model_requests) == 1

    asyncio.run(scenario())


def test_missing_game_context_requests_clarification_without_model(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat", headers=headers, json=message("这个游戏怎么玩？")
            )
            assert response.status_code == 200, response.text
            answer = response.json()
            assert answer["needs_clarification"] is True
            assert answer["game_code"] is None
            assert answer["model"] is None
            assert answer["answer"]
            assert not upstream.model_requests
            assert not prompt_queries(upstream)

    asyncio.run(scenario())


def test_conversation_switches_game_from_explicit_question(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            first = await client.post(
                "/api/v1/chat", headers=headers, json=message("八子归位怎么玩？")
            )
            assert first.status_code == 200, first.text
            conversation_id = first.json()["conversation_id"]
            second = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("再介绍一下鱼的游戏怎么玩。", conversation_id=conversation_id),
            )
            assert second.status_code == 200, second.text
            assert second.json()["conversation_id"] == conversation_id
            assert second.json()["game_code"] == "fish"
            assert second.json()["scene"] == "GAME_QA"
            assert ("game.rules.fish", "1.0.0") in prompt_queries(upstream)
            third = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("那它的获胜条件是什么？", conversation_id=conversation_id),
            )
            assert third.status_code == 200, third.text
            assert third.json()["game_code"] == "fish"

    asyncio.run(scenario())


def test_report_question_uses_saved_facts_and_exact_rule_without_reevaluation(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            job_id = await seed_report(
                container, upstream, facts={"game_rule_version": "historical-v1"}
            )
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("我这局为什么只有铜徽章？", evaluation_id=job_id),
            )
            assert response.status_code == 200, response.text
            answer = response.json()
            assert answer["scene"] == "REPORT_QA"
            assert answer["evaluation_id"] == job_id
            assert answer["game_code"] == "eight-pieces"
            assert answer["game_rule_version"] == "historical-v1"
            assert any(citation["kind"] == "report" for citation in answer["citations"])
            assert ("game.rules.eight-pieces", "historical-v1") in prompt_queries(upstream)
            sent = json.dumps(upstream.model_requests, ensure_ascii=False)
            assert "本局建议先观察目标" in sent
            assert "BRONZE" in sent
            assert "student-1" not in sent and "school-1" not in sent
            assert not [call for call in upstream.calls if call.url.host == "attempts.test"]
            async with container.database.sessions() as session:
                assert await session.scalar(select(func.count()).select_from(EvaluationJob)) == 1

    asyncio.run(scenario())


def test_personal_report_question_selects_latest_finished_game(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            await seed_report(
                container,
                upstream,
                facts={"source_attempt_id": "older-game", "ended_at": "2026-10-25T02:00:00Z"},
                updated_at=datetime(2026, 10, 28, tzinfo=UTC).timestamp(),
            )
            latest_id = await seed_report(
                container,
                upstream,
                facts={"source_attempt_id": "latest-game", "ended_at": "2026-10-26T02:00:00Z"},
                updated_at=datetime(2026, 10, 26, tzinfo=UTC).timestamp(),
            )
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat", headers=headers, json=message("我刚才为什么只有铜徽章？")
            )
            assert response.status_code == 200, response.text
            assert response.json()["scene"] == "REPORT_QA"
            assert response.json()["evaluation_id"] == latest_id
            assert len(upstream.model_requests) == 1

    asyncio.run(scenario())


def test_report_chat_rejects_cross_student_and_cross_school(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            job_id = await seed_report(container, upstream)
            for student, school in [("student-2", "school-1"), ("student-1", "school-2")]:
                headers = await student_headers(client, student, school)
                response = await client.post(
                    "/api/v1/chat",
                    headers=headers,
                    json=message("解释这份报告。", evaluation_id=job_id),
                )
                assert response.status_code == 404, response.text
            assert not upstream.model_requests
            assert not prompt_queries(upstream)

    asyncio.run(scenario())


def test_pending_report_returns_state_without_model_analysis(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            job_id = await seed_report(container, upstream, status="RETRY_WAIT")
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=message("这次测评怎么样？", evaluation_id=job_id),
            )
            assert response.status_code == 200, response.text
            answer = response.json()
            assert answer["scene"] == "REPORT_QA"
            assert answer["evaluation_id"] == job_id
            assert answer["model"] is None
            assert answer["answer"]
            assert not upstream.model_requests
            assert not prompt_queries(upstream)

    asyncio.run(scenario())


def test_missing_report_is_explained_without_fabricated_analysis(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            response = await client.post(
                "/api/v1/chat", headers=headers, json=message("我刚才为什么只有铜徽章？")
            )
            assert response.status_code == 200, response.text
            answer = response.json()
            assert answer["scene"] == "REPORT_QA"
            assert answer["model"] is None
            assert answer["answer"]
            assert not upstream.model_requests
            assert not prompt_queries(upstream)

    asyncio.run(scenario())
