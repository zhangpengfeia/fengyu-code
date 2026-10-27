import asyncio
import json
from datetime import UTC, datetime, timedelta

from src.backends.models import EvaluationJob
from src.schemas.api import AttemptData, Principal
from src.utils.errors import AppError


class MockRuntime:
    def __init__(self, settings):
        self.data = json.loads(settings.mock_data_path.read_text())
        self.revoked_tokens = set()

    @property
    def students(self):
        """Primary mock student first; extra_students are additional test accounts."""
        return [self.data["student"], *self.data.get("extra_students", [])]

    def student_by_id(self, student_id):
        return next((item for item in self.students if item["id"] == student_id), None)

    @property
    def principal(self):
        return Principal(
            app_id="education-platform",
            student_id=self.data["student"]["id"],
            school_id=self.data["school"]["id"],
        )

    async def create_attempt(self, container, user, game_code, attempt_id, *, case=None, age=0):
        container.catalog.get(game_code)
        facts = AttemptData.model_validate(
            {
                **self.data["attempt_defaults"],
                **(case or {}),
                "source_attempt_id": attempt_id,
                "source_revision": 1,
                "student_id": user.student_id,
                "school_id": user.school_id,
                "game_code": game_code,
                "ended_at": (datetime.now(UTC) - timedelta(minutes=age)).isoformat(),
            }
        )
        job = await container.evaluation_repository.enqueue(user.app_id, attempt_id, 1)
        async with container.database.sessions.begin() as session:
            current = await session.get(EvaluationJob, job.id)
            if current.facts:
                if (current.student_id, current.school_id, current.facts["game_code"]) != (
                    user.student_id,
                    user.school_id,
                    game_code,
                ):
                    raise AppError(
                        "IDEMPOTENCY_CONFLICT", "同一结束事件不能关联不同学生或游戏", 409
                    )
            else:
                current.facts = facts.model_dump(mode="json")
                current.student_id, current.school_id = user.student_id, user.school_id
                current.game_code, current.ended_at = game_code, facts.ended_at.timestamp()
        return job.id

    async def initialize(self, container):
        user = self.principal.model_copy(update={"app_id": container.settings.integration_app_id})
        for index, case in enumerate(self.data["seed_reports"]):
            await self.create_attempt(
                container,
                user,
                case["game_code"],
                case["source_attempt_id"],
                case=case,
                age=(index + 1) * 30,
            )
        for _ in self.data["seed_reports"]:
            await container.evaluations.run_once()

    async def worker_loop(self, container):
        while True:
            processed = await container.evaluations.run_once()
            if not processed:
                await asyncio.sleep(0.2)
