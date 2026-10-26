from pathlib import Path
from typing import Literal

from pydantic import Field, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AGENT_", env_file=".env", extra="ignore")

    environment: Literal["development", "production", "test"] = "development"
    database_url: str = "sqlite+aiosqlite:///./data/agent.db"
    integration_app_id: str = "education-platform"
    service_api_key: SecretStr
    jwt_secret: SecretStr
    token_ttl_seconds: int = Field(default=900, ge=60, le=3600)
    cors_origins: list[str] = Field(default_factory=list)
    catalog_path: Path = Path(__file__).resolve().parents[3] / "config/games.yaml"
    nacos_url: str = "http://127.0.0.1:8848"
    nacos_namespace: str = "student-game-agent"
    nacos_username: str = "nacos"
    nacos_password: SecretStr = SecretStr("")
    nacos_access_token: SecretStr = SecretStr("")
    nacos_auth_enabled: bool = True
    nacos_prompt_cache_seconds: int = Field(default=60, ge=0, le=3600)
    base_prompt_key: str = "companion.base"
    base_prompt_version: str = "1.0.0"
    chat_prompt_key: str = "companion.chat"
    chat_prompt_version: str = "1.0.0"
    prompt_prewarm: bool = False
    evaluation_prompt_key: str = "companion.evaluation"
    evaluation_prompt_version: str = "1.0.0"
    llm_base_url: str = "http://127.0.0.1:4000/v1"
    llm_api_key: SecretStr = SecretStr("")
    llm_model: str = ""
    llm_timeout_seconds: int = Field(default=60, ge=1, le=180)
    max_input_chars: int = Field(default=4000, ge=100, le=10000)
    history_turns: int = Field(default=8, ge=0, le=20)
    max_prompt_chars: int = Field(default=30000, ge=1000, le=100000)
    max_output_tokens: int = Field(default=1500, ge=100, le=4096)
    rag_enabled: bool = False
    attempt_api_url: str = ""
    attempt_api_key: SecretStr = SecretStr("")
    worker_poll_seconds: float = Field(default=2, ge=0.1, le=60)
    job_lease_seconds: int = Field(default=300, ge=240, le=3600)
    job_max_attempts: int = Field(default=5, ge=1, le=20)

    @field_validator("service_api_key", "jwt_secret")
    @classmethod
    def strong_secret(cls, value: SecretStr) -> SecretStr:
        if len(value.get_secret_value()) < 32:
            raise ValueError("密钥至少 32 字符，请单独生成，不使用演示口令")
        return value

    @field_validator("nacos_url", "llm_base_url", "attempt_api_url")
    @classmethod
    def valid_server_url(cls, value: str) -> str:
        from urllib.parse import urlsplit

        if value:
            parsed = urlsplit(value)
            if parsed.scheme not in {"http", "https"} or not parsed.hostname:
                raise ValueError("服务地址必须是 HTTP(S) URL")
            if parsed.username or parsed.password or parsed.query or parsed.fragment:
                raise ValueError("服务地址不得包含凭据、查询参数或片段")
        return value.rstrip("/")

    @model_validator(mode="after")
    def validate_options(self) -> "Settings":
        if self.service_api_key == self.jwt_secret:
            raise ValueError("集成凭据和 JWT 签名密钥必须分别生成")
        if self.rag_enabled:
            raise ValueError("RAG 仅预留接口；接入并验证 Retriever 后才能启用")
        if "*" in self.cors_origins:
            raise ValueError("CORS 必须明确列出允许的宿主来源")
        return self
