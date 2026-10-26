from collections.abc import Sequence
from typing import Protocol

import httpx
from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from src.config.settings import Settings
from src.utils.errors import AppError


class ModelGateway(Protocol):
    async def answer(self, messages: Sequence[BaseMessage]) -> str: ...


class OpenAICompatibleGateway:
    """可连接 LiteLLM、其他兼容 API 或内网模型；不向客户端返回推理正文。"""

    def __init__(self, settings: Settings, http_client: httpx.AsyncClient | None = None):
        self.settings = settings
        self.http_client = http_client

    async def answer(self, messages: Sequence[BaseMessage]) -> str:
        cfg = self.settings
        if not cfg.llm_model or not cfg.llm_api_key.get_secret_value():
            raise AppError("MODEL_NOT_CONFIGURED", "请先配置模型名称与访问凭据")
        model = ChatOpenAI(
            model=cfg.llm_model,
            base_url=cfg.llm_base_url,
            api_key=cfg.llm_api_key.get_secret_value(),
            temperature=0.2,
            max_tokens=cfg.max_output_tokens,
            timeout=cfg.llm_timeout_seconds,
            max_retries=0,
            http_async_client=self.http_client,
        )
        try:
            result = await model.ainvoke(list(messages))
            if isinstance(result.content, str):
                answer = result.content
            else:
                answer = "".join(
                    block.get("text", "")
                    for block in result.content
                    if isinstance(block, dict) and block.get("type") == "text"
                )
            if not answer.strip():
                raise AppError("EMPTY_MODEL_RESPONSE", "模型未返回有效回答")
            return answer.strip()
        except AppError:
            raise
        except Exception as exc:
            # 上游错误可能包含 URL、请求内容或凭据，不原样暴露。
            raise AppError("MODEL_UNAVAILABLE", "模型服务暂时不可用，请稍后重试") from exc
