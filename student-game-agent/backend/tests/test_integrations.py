import asyncio
import json

import httpx
import pytest
from conftest import student_headers
from pydantic import ValidationError

from student_game_agent.config.nacos.prompts import NacosPromptRepository
from student_game_agent.config.settings import Settings
from student_game_agent.rag.schemas import RetrievedDocument
from student_game_agent.utils.errors import AppError


def test_nacos_login_refresh_version_and_cache(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):
            cfg = container.settings.model_copy(
                update={
                    "nacos_auth_enabled": True,
                    "nacos_password": container.settings.service_api_key,
                }
            )
            state = {"logins": 0, "queries": 0}

            def handler(request):
                if request.method == "POST":
                    state["logins"] += 1
                    return httpx.Response(
                        200, json={"accessToken": f"token-{state['logins']}", "tokenTtl": 300}
                    )
                state["queries"] += 1
                assert request.url.params["version"] == "1.0.0"
                if request.headers["accessToken"] == "token-1":
                    return httpx.Response(401, json={"code": 401})
                return httpx.Response(
                    200,
                    json={
                        "code": 0,
                        "data": {
                            "promptKey": "rules",
                            "version": "1.0.0",
                            "template": "测试规则。",
                        },
                    },
                )

            async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as http:
                repository = NacosPromptRepository(cfg, http)
                first = await repository.get("rules", "1.0.0")
                assert await repository.get("rules", "1.0.0") == first
                assert state == {"logins": 2, "queries": 2}

    asyncio.run(scenario())


@pytest.mark.parametrize("template", ["", "未填写 {{goal}}"])
def test_empty_or_unresolved_rules_are_rejected(runtime, template):
    async def scenario():
        async with runtime() as (client, container, upstream):
            upstream.nacos_template = template
            with pytest.raises(AppError):
                await container.prompts.get("rules", "1.0.0")
            assert not upstream.model_requests

    asyncio.run(scenario())


def test_rag_extension_receives_scope_and_keeps_image_citations(runtime):
    async def scenario():
        async with runtime() as (client, container, upstream):

            class ScopedRetriever:
                async def retrieve(self, query):
                    assert query.principal.student_id == "student-1"
                    assert query.principal.school_id == "school-1"
                    assert query.game_code == "eight-pieces" and query.rule_version == "1.0.0"
                    return [
                        RetrievedDocument(
                            id="doc-1",
                            kind="image",
                            title="测试图示",
                            text="测试图片的文字说明。",
                            image_object_key="school-1/game/figure.png",
                            source_version="1",
                        )
                    ]

            container.workflow.retriever = ScopedRetriever()
            headers = await student_headers(client)
            from uuid import uuid4

            result = await client.post(
                "/api/v1/chat",
                headers=headers,
                json={
                    "request_id": str(uuid4()),
                    "game_code": "eight-pieces",
                    "question": "解释一下。",
                },
            )
            assert result.status_code == 200
            assert result.json()["citations"][-1]["kind"] == "image"
            sent = json.dumps(upstream.model_requests, ensure_ascii=False)
            assert "测试图片的文字说明" in sent

    asyncio.run(scenario())


def test_unimplemented_rag_cannot_be_silently_enabled():
    with pytest.raises(ValidationError, match="RAG"):
        Settings(_env_file=None, service_api_key="s" * 40, jwt_secret="j" * 40, rag_enabled=True)
