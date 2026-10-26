import asyncio
import logging
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from src.app.container import Container
from src.app.routes import auth, chat, evaluations, games, health
from src.config.settings import Settings
from src.middlewares.http import configure_http
from src.utils.errors import AppError

logger = logging.getLogger(__name__)


def create_app(settings: Settings | None = None, container: Container | None = None) -> FastAPI:
    container = container or Container(settings or Settings())

    @asynccontextmanager
    async def lifespan(app):
        mock_worker = None
        try:
            if container.mock:
                await container.mock.initialize(container)
                if container.settings.mock_worker_enabled:
                    mock_worker = asyncio.create_task(container.mock.worker_loop(container))
            if container.settings.prompt_prewarm:
                try:
                    await container.prompt_resolver.prewarm_common()
                except AppError as exc:
                    logger.warning("prompt_prewarm_failed code=%s", exc.code)
            yield
        finally:
            if mock_worker:
                mock_worker.cancel()
                with suppress(asyncio.CancelledError):
                    await mock_worker
            await container.close()

    app = FastAPI(title="学生游戏 Agent API", version="0.1.0", lifespan=lifespan)
    app.state.container = container
    app.add_middleware(
        CORSMiddleware,
        allow_origins=container.settings.cors_origins,
        allow_credentials=False,
        allow_methods=["GET", "POST"],
        allow_headers=["Authorization", "Content-Type"],
        expose_headers=["X-Request-ID"],
    )
    configure_http(app)
    for router in (health.router, auth.router, games.router, chat.router, evaluations.router):
        app.include_router(router)
    if container.mock:
        from src.mock.routes import router as mock_router

        app.include_router(mock_router)
    return app
