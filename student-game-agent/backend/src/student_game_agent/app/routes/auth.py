from fastapi import APIRouter, Depends

from student_game_agent.app.dependencies import Services
from student_game_agent.schemas.api import TokenRequest
from student_game_agent.security.auth import issue_token, require_service

router = APIRouter(prefix="/internal/v1", tags=["integration"])


@router.post("/tokens", dependencies=[Depends(require_service)])
async def tokens(body: TokenRequest, services: Services):
    return {
        "access_token": issue_token(services.settings, body),
        "token_type": "bearer",
        "expires_in": services.settings.token_ttl_seconds,
    }
