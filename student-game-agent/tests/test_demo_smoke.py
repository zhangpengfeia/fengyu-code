import asyncio

import httpx
import pytest

from src.config.settings import Settings
from src.demo import smoke


def settings(demo):
    return Settings(
        _env_file=None,
        environment="production",
        demo_responses=demo,
        service_api_key="private-integration-key-" + "s" * 32,
        jwt_secret="private-jwt-signing-key-" + "j" * 32,
    )


def test_smoke_uses_only_synthetic_identity_and_never_prints_credentials(capsys):
    async def scenario():
        calls = []

        def upstream(request):
            calls.append(request)
            if request.url.path == "/internal/v1/tokens":
                assert (
                    request.content
                    == b'{"student_id":"deployment-smoke","school_id":"deployment-smoke"}'
                )
                assert (
                    request.headers["X-Integration-Key"]
                    == settings(True).service_api_key.get_secret_value()
                )
                return httpx.Response(200, json={"access_token": "private-token", "demo": True})
            assert request.headers["Authorization"] == "Bearer private-token"
            return httpx.Response(200, json={"answer": "【演示回复】示例内容"})

        async with httpx.AsyncClient(
            base_url="http://127.0.0.1:8088", transport=httpx.MockTransport(upstream)
        ) as client:
            assert await smoke.verify_demo(settings(True), client) is True
        assert len(calls) == 2

    asyncio.run(scenario())
    assert capsys.readouterr() == ("", "")


def test_smoke_skips_all_network_when_demo_is_disabled(capsys):
    async def scenario():
        def no_network(request):
            raise AssertionError("Smoke must skip real model mode")

        async with httpx.AsyncClient(
            base_url="http://127.0.0.1:8088", transport=httpx.MockTransport(no_network)
        ) as client:
            assert await smoke.verify_demo(settings(False), client) is False

    asyncio.run(scenario())
    smoke.main(settings(False))
    assert "已跳过" in capsys.readouterr().out


def test_smoke_failure_never_prints_tokens_headers_or_private_error(monkeypatch, capsys):
    async def fail(_settings):
        raise RuntimeError("private-token private-integration-key X-Integration-Key request-body")

    monkeypatch.setattr(smoke, "verify_demo", fail)
    with pytest.raises(SystemExit) as exception:
        smoke.main(settings(True))
    assert exception.value.code == 1
    output = capsys.readouterr()
    assert output.out == ""
    assert output.err == "Agent 演示问答验收失败，请检查服务状态。\n"
