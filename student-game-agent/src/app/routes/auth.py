from fastapi import APIRouter, Depends

from src.app.dependencies import Services
from src.schemas.api import TokenRequest
from src.security.auth import issue_token, require_service

router = APIRouter(prefix="/internal/v1", tags=["integration"])


@router.post("/tokens", dependencies=[Depends(require_service)])
async def tokens(body: TokenRequest, services: Services):
    return {
        "access_token": issue_token(services.settings, body),
        "token_type": "bearer",
        "expires_in": services.settings.token_ttl_seconds,
    }
