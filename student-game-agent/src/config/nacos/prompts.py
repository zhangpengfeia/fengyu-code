import asyncio
import hashlib
import json
import time

import httpx

from student_game_agent.config.settings import Settings
from student_game_agent.schemas.api import PromptSnapshot
from student_game_agent.utils.errors import AppError


class NacosPromptRepository:
    """只读 Nacos 3.2+ Prompt 客户端；没有空模板或跨版本降级。"""

    def __init__(self, settings: Settings, client: httpx.AsyncClient):
        self.settings = settings
        self.client = client
        self._token = ""
        self._token_until = 0.0
        self._cache: dict[tuple[str, str], tuple[float, PromptSnapshot]] = {}
        self._lock = asyncio.Lock()

    async def _headers(self) -> dict[str, str]:
        cfg = self.settings
        if not cfg.nacos_auth_enabled:
            return {}
        if cfg.nacos_access_token.get_secret_value():
            return {"accessToken": cfg.nacos_access_token.get_secret_value()}
        if self._token and time.monotonic() < self._token_until:
            return {"accessToken": self._token}
        if not cfg.nacos_password.get_secret_value():
            raise AppError("NACOS_NOT_CONFIGURED", "请配置 Nacos 访问凭据")
        response = await self.client.post(
            cfg.nacos_url + "/nacos/v3/auth/user/login",
            data={
                "username": cfg.nacos_username,
                "password": cfg.nacos_password.get_secret_value(),
            },
        )
        response.raise_for_status()
        data = response.json()
        self._token = data["accessToken"]
        self._token_until = time.monotonic() + max(0, float(data["tokenTtl"]) - 30)
        return {"accessToken": self._token}

    async def get(self, key: str, version: str) -> PromptSnapshot:
        async with self._lock:
            cached = self._cache.get((key, version))
            if cached and cached[0] > time.monotonic():
                return cached[1]
            try:
                for attempt in range(2):
                    response = await self.client.get(
                        self.settings.nacos_url + "/nacos/v3/client/ai/prompt",
                        params={
                            "namespaceId": self.settings.nacos_namespace,
                            "promptKey": key,
                            "version": version,
                        },
                        headers=await self._headers(),
                    )
                    payload = response.json()
                    if response.status_code in (401, 403) or payload.get("code") in (401, 403):
                        self._token = ""
                        if attempt == 0:
                            continue
                    response.raise_for_status()
                    if payload.get("code") != 0 or not isinstance(payload.get("data"), dict):
                        raise AppError("RULES_UNAVAILABLE", "当前版本的游戏规则尚未配置或不可用")
                    data = payload["data"]
                    template = data.get("template", "")
                    if (
                        data.get("promptKey") != key
                        or str(data.get("version", "")) != version
                        or not isinstance(template, str)
                        or not template.strip()
                        or len(template) > self.settings.max_prompt_chars
                    ):
                        raise AppError("INVALID_PROMPT", "规则模板为空、过大或版本不匹配")
                    # 本阶段使用完整规则文本，不默默保留未填写变量。
                    if "{{" in template or "}}" in template:
                        raise AppError(
                            "UNRESOLVED_PROMPT", "规则模板存在未填写变量，请先发布完整规则"
                        )
                    canonical = json.dumps(
                        [self.settings.nacos_namespace, key, version, template], ensure_ascii=False
                    )
                    snapshot = PromptSnapshot(
                        namespace=self.settings.nacos_namespace,
                        key=key,
                        version=version,
                        template=template,
                        digest=hashlib.sha256(canonical.encode()).hexdigest(),
                    )
                    self._cache[(key, version)] = (
                        time.monotonic() + self.settings.nacos_prompt_cache_seconds,
                        snapshot,
                    )
                    return snapshot
                raise AppError("NACOS_AUTH_FAILED", "Nacos 认证失败")
            except AppError:
                raise
            except (httpx.HTTPError, ValueError, KeyError, TypeError) as exc:
                raise AppError("NACOS_UNAVAILABLE", "规则服务暂时不可用，请稍后重试") from exc
