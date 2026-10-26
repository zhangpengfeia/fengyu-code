from fastapi import APIRouter
from sqlalchemy import text

from src.app.dependencies import Services
from src.backends.database import SCHEMA_REVISION
from src.utils.errors import AppError

router = APIRouter(prefix="/health", tags=["health"])


@router.get("/live")
async def live():
    return {"status": "UP"}


@router.get("/ready")
async def ready(services: Services):
    try:
        async with services.database.sessions() as session:
            revision = await session.scalar(text("SELECT version_num FROM alembic_version"))
        if revision != SCHEMA_REVISION:
            raise AppError("DATABASE_MIGRATION_REQUIRED", "数据库版本未匹配当前应用，请先执行迁移")
        return {"status": "UP", "external_services": "CHECK_NOT_PERFORMED"}
    except AppError:
        raise
    except Exception as exc:
        raise AppError("DATABASE_NOT_READY", "数据库不可用或尚未迁移") from exc
