from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from student_game_agent.app.container import Container
from student_game_agent.app.routes import auth, chat, evaluations, games, health
from student_game_agent.config.settings import Settings
from student_game_agent.middlewares.http import configure_http


def create_app(settings: Settings | None = None, container: Container | None = None) -> FastAPI:
    container = container or Container(settings or Settings())

    @asynccontextmanager
    async def lifespan(app):
        try:
            yield
        finally:
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
    return app
