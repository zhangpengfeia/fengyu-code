from pathlib import Path

from sqlalchemy.engine import make_url
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

SCHEMA_REVISION = "0002"


class Database:
    def __init__(self, url: str):
        parsed = make_url(url)
        if parsed.drivername == "sqlite+aiosqlite" and parsed.database != ":memory:":
            Path(parsed.database).parent.mkdir(parents=True, exist_ok=True)
        self.engine = create_async_engine(url, pool_pre_ping=True)
        self.sessions = async_sessionmaker(self.engine, expire_on_commit=False)
