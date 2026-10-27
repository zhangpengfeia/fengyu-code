from datetime import UTC, datetime
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Query

from src.app.dependencies import Services, Student
from src.backends.models import EvaluationJob
from src.schemas.api import AttemptEvent, JobResponse
from src.security.auth import require_service
from src.utils.errors import AppError

router = APIRouter(tags=["evaluation"])


def job_response(job: EvaluationJob) -> JobResponse:
    return JobResponse(
        id=job.id,
        source_attempt_id=job.source_attempt_id,
        source_revision=job.source_revision,
        status=job.status,
        attempts=job.attempts,
        error_code=job.error_code,
        report=job.report,
        updated_at=datetime.fromtimestamp(job.updated_at, UTC),
    )


@router.post("/internal/v1/game-events", status_code=202, dependencies=[Depends(require_service)])
async def game_event(body: AttemptEvent, services: Services):
    cfg = services.settings
    if cfg.demo_responses:
        raise AppError("DEMO_EVALUATION_DISABLED", "演示模式仅提供示例问答，暂不接收测评事件")
    if not cfg.attempt_api_url or not cfg.attempt_api_key.get_secret_value():
        raise AppError("ATTEMPT_SOURCE_NOT_CONFIGURED", "尚未配置游戏结果接口，暂不接收评价事件")
    job = await services.evaluation_repository.enqueue(
        cfg.integration_app_id, body.source_attempt_id, body.source_revision
    )
    return {"job_id": job.id, "status": job.status}


@router.get("/api/v1/evaluations/{job_id}", response_model=JobResponse)
async def evaluation(job_id: UUID, user: Student, services: Services):
    return job_response(await services.evaluation_repository.student_job(user, str(job_id)))


@router.get("/api/v1/evaluations", response_model=list[JobResponse])
async def evaluations(
    user: Student,
    services: Services,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
    offset: Annotated[int, Query(ge=0, le=100000)] = 0,
):
    return [
        job_response(job)
        for job in await services.evaluation_repository.student_jobs(user, limit, offset)
    ]
