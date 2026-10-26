.PHONY: install migrate dev worker test check

install:
	uv sync --locked

migrate:
	mkdir -p data
	uv run --locked alembic upgrade head

dev:
	uv run --locked uvicorn student_game_agent.app.main:create_app --factory --host 127.0.0.1 --port 8088 --reload

worker:
	uv run --locked python -m student_game_agent.workers.evaluation_worker

test:
	uv run --locked pytest -q

check:
	uv run --locked ruff check .
	uv run --locked ruff format --check .
