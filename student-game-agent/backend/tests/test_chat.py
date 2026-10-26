import asyncio
import time
from uuid import uuid4

from conftest import student_headers
from sqlalchemy import select, update

from student_game_agent.backends.models import ChatRun, Conversation


def question(**values):
    return {
        "request_id": str(uuid4()),
        "game_code": "eight-pieces",
        "question": "怎么玩？",
        **values,
    }


def test_chat_full_graph_history_snapshots_and_idempotency(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            body = question()
            result = await client.post("/api/v1/chat", headers=headers, json=body)
            assert result.status_code == 200, result.text
            answer = result.json()
            assert answer["answer"] == upstream.model_answer
            assert "PRIVATE_REASONING" not in result.text
            assert len(answer["citations"]) == 2
            assert all(c["version"] == "1.0.0" for c in answer["citations"])
            duplicate = await client.post("/api/v1/chat", headers=headers, json=body)
            assert duplicate.json() == answer
            assert len(upstream.model_requests) == 1
            conflict = await client.post(
                "/api/v1/chat", headers=headers, json={**body, "question": "另一个问题"}
            )
            assert conflict.status_code == 409
            second = await client.post(
                "/api/v1/chat",
                headers=headers,
                json=question(conversation_id=answer["conversation_id"]),
            )
            assert second.status_code == 200
            sent = upstream.model_requests[-1]["messages"]
            assert any(m["role"] == "assistant" and m["content"] == answer["answer"] for m in sent)
            history = await client.get(
                f"/api/v1/conversations/{answer['conversation_id']}/messages", headers=headers
            )
            assert len(history.json()["turns"]) == 2
            async with container.database.sessions() as session:
                run = await session.scalar(
                    select(ChatRun).where(ChatRun.request_id == body["request_id"])
                )
                assert len(run.prompt_snapshots) == 2
                assert run.prompt_snapshots[1]["template"] == upstream.nacos_template

    asyncio.run(scenario())


def test_authentication_cross_student_school_and_game_context(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            assert (await client.get("/api/v1/games")).status_code == 401
            assert (
                await client.post(
                    "/internal/v1/tokens", json={"student_id": "student-1", "school_id": "school-1"}
                )
            ).status_code == 401
            headers = await student_headers(client)
            other = await student_headers(client, "student-2")
            other_school = await student_headers(client, "student-1", "school-2")
            result = await client.post("/api/v1/chat", headers=headers, json=question())
            cid = result.json()["conversation_id"]
            for bad in [other, other_school]:
                assert (
                    await client.get(f"/api/v1/conversations/{cid}/messages", headers=bad)
                ).status_code == 404
                assert (
                    await client.post(
                        "/api/v1/chat", headers=bad, json=question(conversation_id=cid)
                    )
                ).status_code == 404
            assert (
                await client.post(
                    "/api/v1/chat",
                    headers=headers,
                    json=question(conversation_id=cid, game_code="fish"),
                )
            ).status_code == 409
            assert (
                await client.post(
                    "/api/v1/chat", headers=headers, json=question(game_code="unknown-game")
                )
            ).status_code == 404
            assert len(upstream.model_requests) == 1

    asyncio.run(scenario())


def test_rule_failure_releases_lease_and_never_calls_model(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            body = question()
            upstream.nacos_code = 404
            result = await client.post("/api/v1/chat", headers=headers, json=body)
            assert result.status_code == 503
            assert not upstream.model_requests
            async with container.database.sessions() as session:
                run = await session.scalar(select(ChatRun))
                assert run.status == "FAILED"
            upstream.nacos_code = 0
            assert (
                await client.post("/api/v1/chat", headers=headers, json=body)
            ).status_code == 200

    asyncio.run(scenario())


def test_busy_conversation_and_expired_lease_recovery(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            first = (await client.post("/api/v1/chat", headers=headers, json=question())).json()
            cid = first["conversation_id"]
            async with container.database.sessions.begin() as session:
                await session.execute(
                    update(Conversation)
                    .where(Conversation.id == cid)
                    .values(busy_until=time.time() + 100, busy_token="busy")
                )
            body = question(conversation_id=cid)
            assert (
                await client.post("/api/v1/chat", headers=headers, json=body)
            ).status_code == 409
            async with container.database.sessions.begin() as session:
                await session.execute(
                    update(Conversation)
                    .where(Conversation.id == cid)
                    .values(busy_until=time.time() - 1)
                )
            assert (
                await client.post("/api/v1/chat", headers=headers, json=body)
            ).status_code == 200

    asyncio.run(scenario())


def test_sse_and_sanitized_validation(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            headers = await student_headers(client)
            result = await client.post("/api/v1/chat/stream", headers=headers, json=question())
            assert result.status_code == 200
            assert "event: status" in result.text
            assert "event: answer" in result.text
            assert "event: done" in result.text
            invalid = await client.post(
                "/api/v1/chat", headers=headers, json=question(secret="DO_NOT_ECHO")
            )
            assert invalid.status_code == 422
            assert "DO_NOT_ECHO" not in invalid.text
            assert result.headers["x-request-id"]

    asyncio.run(scenario())
