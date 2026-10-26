import asyncio
from contextlib import asynccontextmanager
from pathlib import Path
from uuid import uuid4

import httpx
import pytest
from alembic import command
from alembic.config import Config
from pydantic import ValidationError

from src.app.container import Container
from src.app.main import create_app
from src.config.settings import Settings

ROOT = Path(__file__).resolve().parents[1]


@asynccontextmanager
async def mock_client(tmp_path):
    settings = Settings(
        _env_file=None,
        mock_mode=True,
        environment="test",
        service_api_key="s" * 40,
        jwt_secret="j" * 40,
        database_url=f"sqlite+aiosqlite:///{tmp_path}/mock.db",
    )
    migration = Config(str(ROOT / "alembic.ini"))
    migration.attributes["settings"] = settings
    await asyncio.to_thread(command.upgrade, migration, "head")

    def no_network(request):
        raise AssertionError("Mock 模式不应访问任何外部网络")

    container = Container(settings, httpx.AsyncClient(transport=httpx.MockTransport(no_network)))
    app = create_app(container=container)
    async with app.router.lifespan_context(app):
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app), base_url="http://test"
        ) as client:
            yield client, container


async def login(client):
    response = await client.post(
        "/dev/education/biz/auth/login",
        json={"mode": "STUDENT", "username": "demo", "password": "demo123", "school_id": "11000"},
    )
    assert response.status_code == 200
    edu = {"Authorization": "Bearer " + response.json()["data"]["token"]}
    exchange = await client.post("/dev/education/biz/auth/agent-token", headers=edu)
    assert exchange.status_code == 200
    return edu, {"Authorization": "Bearer " + exchange.json()["data"]["access_token"]}


def test_mock_login_chat_report_and_game_end_use_real_backend_flow(tmp_path):
    async def scenario():
        async with mock_client(tmp_path) as (client, container):
            assert (await client.get("/health/ready")).status_code == 200
            assert (
                await client.post(
                    "/dev/mock/game-finish",
                    json={"request_id": str(uuid4()), "game_code": "eight-pieces"},
                )
            ).status_code == 401
            edu, agent = await login(client)
            assert (await client.get("/dev/education/biz/auth/me", headers=edu)).json()["data"][
                "name"
            ] == "联调同学"
            assert (await client.get("/dev/education/biz/options", headers=edu)).status_code == 200
            courses = await client.get("/dev/education/biz/catalog/courseware", headers=edu)
            assert len(courses.json()["rows"]) == 2
            chat = await client.post(
                "/api/v1/chat", headers=agent, json={"request_id": str(uuid4()), "question": "你好"}
            )
            assert chat.status_code == 200
            assert chat.json()["model"] == "mock-local"
            assert "模拟联调回复" in chat.json()["answer"]
            assert chat.headers["x-data-mode"] == "mock"
            reports = (await client.get("/api/v1/evaluations", headers=agent)).json()
            assert len(reports) == 2
            assert reports[0]["report"]["generation_mode"] == "MOCK"
            body = {"request_id": str(uuid4()), "game_code": "eight-pieces"}
            result = await client.post("/dev/mock/game-finish", headers=agent, json=body)
            assert result.status_code == 202
            job_id = result.json()["job_id"]
            assert (await client.post("/dev/mock/game-finish", headers=agent, json=body)).json()[
                "job_id"
            ] == job_id
            for _ in range(30):
                report = (await client.get(f"/api/v1/evaluations/{job_id}", headers=agent)).json()
                if report["status"] == "COMPLETED":
                    break
                await asyncio.sleep(0.05)
            assert report["status"] == "COMPLETED"
            assert report["report"]["generation_mode"] == "MOCK"
            follow = await client.post(
                "/api/v1/chat/stream",
                headers=agent,
                json={
                    "request_id": str(uuid4()),
                    "evaluation_id": job_id,
                    "question": "我这局为什么只有铜徽章？",
                },
            )
            assert "event: answer" in follow.text and "REPORT_QA" in follow.text
            assert len((await client.get("/api/v1/evaluations", headers=agent)).json()) == 3
            assert (await client.post("/dev/education/logout", headers=edu)).status_code == 200
            assert (
                await client.post("/dev/education/biz/auth/agent-token", headers=edu)
            ).status_code == 401

    asyncio.run(scenario())


def test_mock_mode_cannot_start_in_production():
    with pytest.raises(ValidationError, match="生产环境"):
        Settings(
            _env_file=None,
            mock_mode=True,
            environment="production",
            service_api_key="s" * 40,
            jwt_secret="j" * 40,
        )


def test_live_mode_does_not_expose_mock_routes(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            assert (await client.get("/dev/education/captchaImage")).status_code == 404
            assert (await client.post("/dev/mock/game-finish", json={})).status_code == 404
            assert (await client.post("/dev/mock/widget-session", json={})).status_code == 404

    asyncio.run(scenario())


def test_planet_widget_accepts_host_scores_and_reuses_the_same_round(tmp_path):
    async def scenario():
        async with mock_client(tmp_path) as (client, _container):
            request_id = str(uuid4())
            body = {
                "request_id": request_id,
                "facts": {
                    "source_attempt_id": "planet-widget-" + request_id,
                    "source_revision": 1,
                    "student_id": "mock-student-001",
                    "school_id": "11000",
                    "game_code": "eight-pieces",
                    "game_rule_version": "1.0.0",
                    "volume_no": 7,
                    "mode": "ASSESSMENT",
                    "difficulty": "NOVICE",
                    "duration_ms": 142000,
                    "actual_steps": 49,
                    "optimal_steps": 28,
                    "badge": "SILVER",
                    "passed": True,
                    "ended_at": datetime_now(),
                },
            }
            first = await client.post("/dev/mock/widget-session", json=body)
            assert first.status_code == 200, first.text
            session = first.json()
            assert session["mock"] is True
            assert session["facts"]["actual_steps"] == 49
            assert session["facts"]["duration_ms"] == 142000
            assert session["facts"]["volume_no"] == 7
            duplicate = await client.post("/dev/mock/widget-session", json=body)
            assert duplicate.json()["evaluation_id"] == session["evaluation_id"]
            headers = {"Authorization": "Bearer " + session["access_token"]}
            for _ in range(40):
                report = (
                    await client.get(
                        "/api/v1/evaluations/" + session["evaluation_id"], headers=headers
                    )
                ).json()
                if report["status"] == "COMPLETED":
                    break
                await asyncio.sleep(0.05)
            assert report["status"] == "COMPLETED"
            assert report["report"]["facts"]["actual_steps"] == 49
            assert report["report"]["facts"]["badge"] == "SILVER"
            answer = await client.post(
                "/api/v1/chat",
                headers=headers,
                json={
                    "request_id": str(uuid4()),
                    "evaluation_id": session["evaluation_id"],
                    "game_code": "eight-pieces",
                    "question": "我这局的步数和用时是多少？",
                },
            )
            assert answer.status_code == 200, answer.text
            assert answer.json()["scene"] == "REPORT_QA"
            assert "49" in answer.json()["answer"]
            assert "142" in answer.json()["answer"]
            changed = {**body, "facts": {**body["facts"], "actual_steps": 50}}
            assert (await client.post("/dev/mock/widget-session", json=changed)).status_code == 409
            foreign = {**body, "facts": {**body["facts"], "student_id": "another-student"}}
            assert (await client.post("/dev/mock/widget-session", json=foreign)).status_code == 403
            invalid = {**body, "facts": {**body["facts"], "passed": False}}
            assert (await client.post("/dev/mock/widget-session", json=invalid)).status_code == 422
            training_id = str(uuid4())
            training = {
                "request_id": training_id,
                "facts": {
                    **body["facts"],
                    "source_attempt_id": "planet-widget-" + training_id,
                    "mode": "TRAINING",
                },
            }
            trained = await client.post("/dev/mock/widget-session", json=training)
            assert trained.status_code == 200, trained.text
            assert trained.json()["evaluation_id"] is None
            assert len((await client.get("/api/v1/evaluations", headers=headers)).json()) == 3

    asyncio.run(scenario())


def datetime_now():
    from datetime import UTC, datetime

    return datetime.now(UTC).isoformat()
