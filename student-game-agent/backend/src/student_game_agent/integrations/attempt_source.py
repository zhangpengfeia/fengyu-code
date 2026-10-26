from typing import Protocol
from urllib.parse import quote

import httpx
from pydantic import ValidationError

from student_game_agent.config.settings import Settings
from student_game_agent.schemas.api import AttemptData
from student_game_agent.utils.errors import AppError


class AttemptSource(Protocol):
    async def fetch(self, attempt_id: str, revision: int) -> AttemptData: ...


class HttpAttemptSource:
    """仅调用服务端配置的权威接口，回调不携带可伪造成绩的事实字段。"""

    def __init__(self, settings: Settings, client: httpx.AsyncClient):
        self.settings, self.client = settings, client

    async def fetch(self, attempt_id: str, revision: int) -> AttemptData:
        cfg = self.settings
        if not cfg.attempt_api_url or not cfg.attempt_api_key.get_secret_value():
            raise AppError("ATTEMPT_SOURCE_NOT_CONFIGURED", "尚未配置权威游戏结果接口")
        try:
            response = await self.client.get(
                cfg.attempt_api_url + "/attempts/" + quote(attempt_id, safe=""),
                params={"revision": revision},
                headers={"Authorization": "Bearer " + cfg.attempt_api_key.get_secret_value()},
            )
            if response.status_code in (202, 404):
                raise AppError("ATTEMPT_NOT_READY", "本局结果尚未就绪")
            response.raise_for_status()
            facts = AttemptData.model_validate(response.json())
            if facts.source_attempt_id != attempt_id or facts.source_revision != revision:
                raise AppError("ATTEMPT_REVISION_MISMATCH", "来源返回的本局 ID 或数据版本不一致")
            return facts
        except AppError:
            raise
        except ValidationError as exc:
            raise AppError("INVALID_ATTEMPT_DATA", "本局数据字段不完整或不一致") from exc
        except (httpx.HTTPError, ValueError) as exc:
            raise AppError("ATTEMPT_SOURCE_UNAVAILABLE", "本局数据接口暂时不可用") from exc
