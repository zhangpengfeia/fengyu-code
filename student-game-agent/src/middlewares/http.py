import logging
from uuid import uuid4

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from student_game_agent.utils.errors import AppError

logger = logging.getLogger(__name__)


def configure_http(app):
    @app.middleware("http")
    async def request_context(request: Request, call_next):
        request.state.request_id = str(uuid4())
        response = await call_next(request)
        response.headers["X-Request-ID"] = request.state.request_id
        response.headers["Cache-Control"] = "no-store"
        return response

    @app.exception_handler(AppError)
    async def app_error(request, exc: AppError):
        return JSONResponse(
            status_code=exc.status,
            content={
                "error": {"code": exc.code, "message": exc.message},
                "request_id": request.state.request_id,
            },
        )

    @app.exception_handler(RequestValidationError)
    async def invalid_request(request, exc):
        # Pydantic 默认错误可能包含原始凭据或学生文本，不回显 input。
        return JSONResponse(
            status_code=422,
            content={
                "error": {"code": "INVALID_REQUEST", "message": "请求字段或格式不符合接口要求"},
                "request_id": request.state.request_id,
            },
        )

    @app.exception_handler(Exception)
    async def unexpected_error(request, exc):
        logger.error(
            "request_failed request_id=%s type=%s", request.state.request_id, type(exc).__name__
        )
        return JSONResponse(
            status_code=500,
            content={
                "error": {"code": "INTERNAL_ERROR", "message": "服务暂时不可用"},
                "request_id": request.state.request_id,
            },
        )
