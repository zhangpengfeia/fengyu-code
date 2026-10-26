from fastapi import APIRouter
from sqlalchemy import text

from student_game_agent.app.dependencies import Services
from student_game_agent.utils.errors import AppError

router = APIRouter(prefix="/health", tags=["health"])


@router.get("/live")
async def live():
    return {"status": "UP"}


@router.get("/ready")
async def ready(services: Services):
    try:
        async with services.database.sessions() as session:
            await session.execute(text("SELECT version_num FROM alembic_version"))
        return {"status": "UP", "external_services": "CHECK_NOT_PERFORMED"}
    except Exception as exc:
        raise AppError("DATABASE_NOT_READY", "数据库不可用或尚未迁移") from exc
