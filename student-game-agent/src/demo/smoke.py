"""Container-only demonstration acceptance check; never prints credentials or response bodies."""

import asyncio
import sys
from uuid import uuid4

import httpx

from src.config.settings import Settings


async def verify_demo(settings: Settings, client: httpx.AsyncClient | None = None) -> bool:
    if not settings.demo_responses:
        return False
    if client is None:
        async with httpx.AsyncClient(
            base_url="http://127.0.0.1:8088", timeout=15, trust_env=False, follow_redirects=False
        ) as owned:
            return await verify_demo(settings, owned)
    response = await client.post(
        "/internal/v1/tokens",
        headers={"X-Integration-Key": settings.service_api_key.get_secret_value()},
        json={"student_id": "deployment-smoke", "school_id": "deployment-smoke"},
    )
    response.raise_for_status()
    session = response.json()
    if session.get("demo") is not True or not isinstance(session.get("access_token"), str):
        raise RuntimeError("DEMO_SMOKE_FAILED")
    response = await client.post(
        "/api/v1/chat",
        headers={"Authorization": "Bearer " + session["access_token"]},
        json={"request_id": str(uuid4()), "question": "我们一起练习观察和思考吧"},
    )
    response.raise_for_status()
    answer = response.json()
    if not str(answer.get("answer", "")).startswith("【演示回复】"):
        raise RuntimeError("DEMO_SMOKE_FAILED")
    return True


def main(settings: Settings | None = None) -> None:
    try:
        checked = asyncio.run(verify_demo(settings or Settings()))
    except Exception:
        print("Agent 演示问答验收失败，请检查服务状态。", file=sys.stderr)
        raise SystemExit(1) from None
    print("Agent 演示问答验收通过。" if checked else "Agent 演示模式未启用，已跳过示例问答验收。")


if __name__ == "__main__":
    main()
