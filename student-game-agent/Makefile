.PHONY: install migrate dev worker test check mock mock-api

install:
	uv sync --locked

migrate:
	mkdir -p data
	uv run --locked alembic upgrade head

dev:
	uv run --locked uvicorn src.app.main:create_app --factory --host 127.0.0.1 --port 8088 --reload

worker:
	uv run --locked python -m src.workers.evaluation_worker

mock:
	uv run --locked python scripts/dev_mock.py

mock-api:
	uv run --locked python scripts/dev_mock.py --api-only

test:
	uv run --locked pytest -q

check:
	uv run --locked ruff check .
	uv run --locked ruff format --check .
