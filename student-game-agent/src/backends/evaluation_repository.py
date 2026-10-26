import time
from uuid import uuid4

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError

from src.backends.models import EvaluationJob
from src.schemas.api import Principal
from src.utils.errors import AppError


class EvaluationRepository:
    def __init__(self, database):
        self.sessions = database.sessions

    async def enqueue(self, app_id: str, attempt_id: str, revision: int) -> EvaluationJob:
        async def existing():
            async with self.sessions() as session:
                return await session.scalar(
                    select(EvaluationJob).where(
                        EvaluationJob.app_id == app_id,
                        EvaluationJob.source_attempt_id == attempt_id,
                        EvaluationJob.source_revision == revision,
                    )
                )

        job = await existing()
        if job:
            return job
        try:
            async with self.sessions.begin() as session:
                job = EvaluationJob(
                    id=str(uuid4()),
                    app_id=app_id,
                    source_attempt_id=attempt_id,
                    source_revision=revision,
                )
                session.add(job)
            return job
        except IntegrityError:
            return await existing()

    async def claim_job(self, lease_seconds: int) -> EvaluationJob | None:
        now = time.time()
        async with self.sessions.begin() as session:
            eligible = (
                EvaluationJob.status.in_(["RECEIVED", "WAITING_DATA", "RETRY_WAIT", "PROCESSING"])
                & (EvaluationJob.available_at <= now)
                & (EvaluationJob.lease_until <= now)
            )
            job_id = await session.scalar(
                select(EvaluationJob.id)
                .where(eligible)
                .order_by(EvaluationJob.available_at, EvaluationJob.id)
                .limit(1)
            )
            if not job_id:
                return None
            changed = await session.execute(
                update(EvaluationJob)
                .where(EvaluationJob.id == job_id, eligible)
                .values(
                    status="PROCESSING",
                    attempts=EvaluationJob.attempts + 1,
                    lease_until=now + lease_seconds,
                    execution_token=str(uuid4()),
                    updated_at=now,
                )
            )
            if changed.rowcount != 1:
                return None
            return await session.get(EvaluationJob, job_id)

    async def update_job(self, job: EvaluationJob, **values) -> bool:
        async with self.sessions.begin() as session:
            changed = await session.execute(
                update(EvaluationJob)
                .where(
                    EvaluationJob.id == job.id, EvaluationJob.execution_token == job.execution_token
                )
                .values(updated_at=time.time(), **values)
            )
            return changed.rowcount == 1

    async def student_job(self, user: Principal, job_id: str) -> EvaluationJob:
        async with self.sessions() as session:
            job = await session.get(EvaluationJob, job_id)
            if not job or (job.app_id, job.student_id, job.school_id) != (
                user.app_id,
                user.student_id,
                user.school_id,
            ):
                raise AppError("REPORT_NOT_FOUND", "评价记录不存在或尚未确认归属", 404)
            return job

    async def student_jobs(self, user: Principal, limit: int, offset: int) -> list[EvaluationJob]:
        async with self.sessions() as session:
            return list(
                (
                    await session.scalars(
                        select(EvaluationJob)
                        .where(
                            EvaluationJob.app_id == user.app_id,
                            EvaluationJob.student_id == user.student_id,
                            EvaluationJob.school_id == user.school_id,
                        )
                        .order_by(EvaluationJob.updated_at.desc(), EvaluationJob.id.desc())
                        .offset(offset)
                        .limit(limit)
                    )
                ).all()
            )

    async def latest_student_job(
        self, user: Principal, game_code: str | None = None
    ) -> EvaluationJob | None:
        async with self.sessions() as session:
            query = select(EvaluationJob).where(
                EvaluationJob.app_id == user.app_id,
                EvaluationJob.student_id == user.student_id,
                EvaluationJob.school_id == user.school_id,
                EvaluationJob.ended_at.is_not(None),
                EvaluationJob.status != "SKIPPED_TRAINING",
            )
            if game_code:
                query = query.where(EvaluationJob.game_code == game_code)
            return await session.scalar(
                query.order_by(
                    EvaluationJob.ended_at.desc(),
                    EvaluationJob.source_revision.desc(),
                    EvaluationJob.updated_at.desc(),
                    EvaluationJob.id.desc(),
                ).limit(1)
            )
