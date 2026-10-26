FROM python:3.13.11-slim
COPY --from=ghcr.io/astral-sh/uv:0.11.18 /uv /usr/local/bin/uv
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 UV_LINK_MODE=copy
WORKDIR /app
COPY backend/pyproject.toml backend/uv.lock ./
COPY backend/src ./src
COPY backend/config ./config
COPY backend/alembic.ini ./
COPY backend/migrations ./migrations
RUN uv sync --locked --no-dev && groupadd -g 10001 agent && useradd -u 10001 -g agent agent
USER agent
EXPOSE 8088
CMD ["/app/.venv/bin/uvicorn", "student_game_agent.app.main:create_app", "--factory", "--host", "0.0.0.0", "--port", "8088"]
