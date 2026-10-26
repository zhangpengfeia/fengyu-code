from fastapi import APIRouter

from src.app.dependencies import Services, Student

router = APIRouter(prefix="/api/v1/games", tags=["games"])


@router.get("")
async def games(user: Student, services: Services):
    return {
        "games": [game.model_dump() for game in services.catalog.games.values() if game.enabled]
    }
