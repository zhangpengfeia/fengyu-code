import json
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse

from student_game_agent.app.dependencies import Services, Student
from student_game_agent.schemas.api import ChatRequest, ChatResponse
from student_game_agent.utils.errors import AppError

router = APIRouter(prefix="/api/v1", tags=["chat"])


@router.post("/chat", response_model=ChatResponse)
async def chat(body: ChatRequest, user: Student, services: Services):
    return await services.chat.answer(user, body)


@router.post("/chat/stream")
async def stream(body: ChatRequest, user: Student, services: Services):
    # 当前提供处理状态和完整最终回答；不声称逐 Token 输出。
    async def events():
        yield 'event: status\ndata: {"status":"PROCESSING"}\n\n'
        try:
            result = await services.chat.answer(user, body)
            yield "event: answer\ndata: " + result.model_dump_json() + "\n\n"
            yield "event: done\ndata: {}\n\n"
        except AppError as exc:
            yield (
                "event: error\ndata: "
                + json.dumps({"code": exc.code, "message": exc.message}, ensure_ascii=False)
                + "\n\n"
            )

    return StreamingResponse(
        events(), media_type="text/event-stream", headers={"X-Accel-Buffering": "no"}
    )


@router.get("/conversations/{conversation_id}/messages")
async def messages(
    conversation_id: UUID,
    user: Student,
    services: Services,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
):
    return {
        "turns": await services.chat_repository.owned_history(user, str(conversation_id), limit)
    }
