import secrets
from datetime import UTC, datetime, timedelta
from uuid import uuid4

import jwt
from fastapi import Header, Request

from student_game_agent.schemas.api import Principal, TokenRequest
from student_game_agent.utils.errors import AppError


def require_service(request: Request, x_integration_key: str | None = Header(default=None)) -> None:
    expected = request.app.state.container.settings.service_api_key.get_secret_value()
    if not x_integration_key or not secrets.compare_digest(x_integration_key, expected):
        raise AppError("UNAUTHORIZED", "集成服务认证失败", 401)


def issue_token(settings, body: TokenRequest) -> str:
    now = datetime.now(UTC)
    return jwt.encode(
        {
            "sub": body.student_id,
            "school_id": body.school_id,
            "app_id": settings.integration_app_id,
            "iss": "student-game-agent",
            "aud": "student-game-agent-api",
            "iat": now,
            "exp": now + timedelta(seconds=settings.token_ttl_seconds),
            "jti": str(uuid4()),
        },
        settings.jwt_secret.get_secret_value(),
        algorithm="HS256",
    )


def current_principal(
    request: Request, authorization: str | None = Header(default=None)
) -> Principal:
    settings = request.app.state.container.settings
    if not authorization or not authorization.startswith("Bearer "):
        raise AppError("UNAUTHORIZED", "请提供有效的学生访问令牌", 401)
    try:
        claims = jwt.decode(
            authorization[7:],
            settings.jwt_secret.get_secret_value(),
            algorithms=["HS256"],
            issuer="student-game-agent",
            audience="student-game-agent-api",
            options={"require": ["sub", "exp", "iat", "jti", "school_id", "app_id"]},
        )
        if claims["app_id"] != settings.integration_app_id:
            raise ValueError("wrong application")
        return Principal(
            app_id=claims["app_id"], student_id=claims["sub"], school_id=claims["school_id"]
        )
    except (jwt.PyJWTError, ValueError, TypeError, KeyError) as exc:
        raise AppError("UNAUTHORIZED", "学生访问令牌无效或已过期", 401) from exc
