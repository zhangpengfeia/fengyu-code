import asyncio
import json
from contextlib import asynccontextmanager
from pathlib import Path

import httpx
import pytest
from alembic import command
from alembic.config import Config

from student_game_agent.app.container import Container
from student_game_agent.app.main import create_app
from student_game_agent.config.settings import Settings

BACKEND_ROOT = Path(__file__).resolve().parents[1]


class Upstream:
    """协议测试服务器；规则与测评结果是测试数据，不是实际游戏规则。"""

    def __init__(self):
        self.calls = []
        self.model_requests = []
        self.model_answer = "请先观察游戏目标，再根据规则逐步操作。"
        self.model_status = 200
        self.nacos_code = 0
        self.nacos_template = "测试专用规则：只能移动相邻格。"
        self.source_status = 200
        self.facts = {
            "source_attempt_id": "attempt-1",
            "source_revision": 1,
            "student_id": "student-1",
            "school_id": "school-1",
            "game_code": "eight-pieces",
            "game_rule_version": "1.0.0",
            "volume_no": 5,
            "mode": "ASSESSMENT",
            "difficulty": "NOVICE",
            "duration_ms": 83000,
            "actual_steps": 36,
            "optimal_steps": 28,
            "badge": "BRONZE",
            "passed": True,
            "ended_at": "2026-10-26T02:00:00Z",
        }

    def handle(self, request):
        self.calls.append(request)
        if request.url.host == "nacos.test":
            assert request.url.path == "/nacos/v3/client/ai/prompt"
            return httpx.Response(
                200,
                json={
                    "code": self.nacos_code,
                    "data": {
                        "promptKey": request.url.params["promptKey"],
                        "version": request.url.params["version"],
                        "template": self.nacos_template,
                    },
                },
            )
        if request.url.host == "llm.test":
            self.model_requests.append(json.loads(request.content))
            if self.model_status != 200:
                return httpx.Response(self.model_status, json={"error": {"message": "upstream"}})
            return httpx.Response(
                200,
                json={
                    "id": "chatcmpl-test",
                    "object": "chat.completion",
                    "created": 1,
                    "model": "test-model",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": self.model_answer,
                                "reasoning_content": "PRIVATE_REASONING_DO_NOT_RETURN",
                            },
                            "finish_reason": "stop",
                        }
                    ],
                    "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20},
                },
            )
        if request.url.host == "attempts.test":
            return httpx.Response(self.source_status, json=self.facts)
        raise AssertionError(f"Unexpected upstream {request.url.host}")


@pytest.fixture
def runtime(tmp_path):
    @asynccontextmanager
    async def start(**overrides):
        cfg = Settings(
            _env_file=None,
            environment="test",
            service_api_key="s" * 40,
            jwt_secret="j" * 40,
            database_url=f"sqlite+aiosqlite:///{tmp_path / 'agent.db'}",
            nacos_url="http://nacos.test",
            nacos_auth_enabled=False,
            llm_base_url="http://llm.test/v1",
            llm_api_key="test-model-key",
            llm_model="test-model",
            attempt_api_url="http://attempts.test",
            attempt_api_key="source-key",
            **overrides,
        )
        migration = Config(str(BACKEND_ROOT / "alembic.ini"))
        migration.attributes["settings"] = cfg
        await asyncio.to_thread(command.upgrade, migration, "head")
        upstream = Upstream()
        container = Container(
            cfg, httpx.AsyncClient(transport=httpx.MockTransport(upstream.handle))
        )
        app = create_app(container=container)
        async with app.router.lifespan_context(app):
            async with httpx.AsyncClient(
                transport=httpx.ASGITransport(app=app), base_url="http://agent.test"
            ) as client:
                yield client, container, upstream

    return start


async def student_headers(client, student="student-1", school="school-1"):
    response = await client.post(
        "/internal/v1/tokens",
        headers={"X-Integration-Key": "s" * 40},
        json={"student_id": student, "school_id": school},
    )
    assert response.status_code == 200, response.text
    return {"Authorization": "Bearer " + response.json()["access_token"]}
