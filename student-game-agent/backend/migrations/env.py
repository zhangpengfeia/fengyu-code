import asyncio

from alembic import context
from sqlalchemy.ext.asyncio import create_async_engine

from student_game_agent.backends.models import Base
from student_game_agent.config.settings import Settings

settings = context.config.attributes.get("settings") or Settings()


def do_migrate(connection):
    context.configure(connection=connection, target_metadata=Base.metadata, compare_type=True)
    with context.begin_transaction():
        context.run_migrations()


async def run_online():
    engine = create_async_engine(settings.database_url)
    try:
        async with engine.connect() as connection:
            await connection.run_sync(do_migrate)
    finally:
        await engine.dispose()


if context.is_offline_mode():
    context.configure(url=settings.database_url, target_metadata=Base.metadata, literal_binds=True)
    with context.begin_transaction():
        context.run_migrations()
else:
    asyncio.run(run_online())
