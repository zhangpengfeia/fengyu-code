import hashlib
import json
import time
from uuid import uuid4

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError

from student_game_agent.backends.models import ChatRun, Conversation
from student_game_agent.schemas.api import ChatRequest, ChatResponse, Principal
from student_game_agent.utils.errors import AppError


def digest(value) -> str:
    return hashlib.sha256(
        json.dumps(value, sort_keys=True, ensure_ascii=False).encode()
    ).hexdigest()


def owned(conversation: Conversation, user: Principal) -> bool:
    return (
        conversation.app_id == user.app_id
        and conversation.student_id == user.student_id
        and conversation.school_id == user.school_id
    )


class ChatRepository:
    def __init__(self, database):
        self.sessions = database.sessions

    async def begin_chat(self, user: Principal, body: ChatRequest, lease_seconds: int) -> dict:
        key = digest([user.model_dump(), str(body.request_id)])
        payload_hash = digest(body.model_dump(mode="json"))
        now = time.time()
        token = str(uuid4())
        try:
            async with self.sessions.begin() as session:
                run = await session.get(ChatRun, key)
                if run:
                    if run.payload_hash != payload_hash:
                        raise AppError("IDEMPOTENCY_CONFLICT", "同一请求 ID 不能提交不同问题", 409)
                    if run.status == "COMPLETED":
                        return {"response": ChatResponse.model_validate(run.response)}
                    if run.status == "RUNNING" and run.lease_until > now:
                        raise AppError("RUN_IN_PROGRESS", "本次问题仍在处理，请稍后查询", 409)
                    conversation = await session.get(Conversation, run.conversation_id)
                elif body.conversation_id:
                    conversation = await session.get(Conversation, str(body.conversation_id))
                else:
                    conversation = Conversation(
                        id=str(uuid4()), **user.model_dump(), game_code=body.game_code
                    )
                    session.add(conversation)
                    await session.flush()
                if not conversation or not owned(conversation, user):
                    raise AppError("CONVERSATION_NOT_FOUND", "会话不存在", 404)
                if conversation.game_code != body.game_code:
                    raise AppError("GAME_CONTEXT_MISMATCH", "切换游戏请创建新会话", 409)
                claimed = await session.execute(
                    update(Conversation)
                    .where(Conversation.id == conversation.id, Conversation.busy_until <= now)
                    .values(busy_until=now + lease_seconds, busy_token=token)
                )
                if claimed.rowcount != 1:
                    raise AppError("CONVERSATION_BUSY", "请等待当前会话回答完成", 409)
                if run:
                    run.status = "RUNNING"
                    run.execution_token = token
                    run.lease_until = now + lease_seconds
                else:
                    run = ChatRun(
                        id=key,
                        conversation_id=conversation.id,
                        request_id=str(body.request_id),
                        payload_hash=payload_hash,
                        question=body.question,
                        status="RUNNING",
                        execution_token=token,
                        lease_until=now + lease_seconds,
                    )
                    session.add(run)
                return {"key": key, "token": token, "conversation_id": conversation.id}
        except IntegrityError as exc:
            raise AppError("REQUEST_CONFLICT", "请求已提交，请使用原请求 ID 重试", 409) from exc

    async def history(self, conversation_id: str, turns: int) -> list[dict]:
        if turns == 0:
            return []
        async with self.sessions() as session:
            runs = (
                await session.scalars(
                    select(ChatRun)
                    .where(
                        ChatRun.conversation_id == conversation_id, ChatRun.status == "COMPLETED"
                    )
                    .order_by(ChatRun.created_at.desc(), ChatRun.id.desc())
                    .limit(turns)
                )
            ).all()
            return [
                {"question": run.question, "answer": run.response["answer"]}
                for run in reversed(runs)
            ]

    async def finish_chat(
        self, claim: dict, response: ChatResponse | None, snapshots: list
    ) -> None:
        async with self.sessions.begin() as session:
            changed = await session.execute(
                update(ChatRun)
                .where(ChatRun.id == claim["key"], ChatRun.execution_token == claim["token"])
                .values(
                    status="COMPLETED" if response else "FAILED",
                    response=response.model_dump(mode="json") if response else None,
                    prompt_snapshots=snapshots,
                )
            )
            if changed.rowcount != 1:
                raise AppError("LEASE_LOST", "请求执行权已变更，请查询最新结果", 409)
            await session.execute(
                update(Conversation)
                .where(
                    Conversation.id == claim["conversation_id"],
                    Conversation.busy_token == claim["token"],
                )
                .values(busy_until=0, busy_token="")
            )

    async def owned_history(self, user: Principal, conversation_id: str, turns: int) -> list[dict]:
        async with self.sessions() as session:
            conversation = await session.get(Conversation, conversation_id)
            if not conversation or not owned(conversation, user):
                raise AppError("CONVERSATION_NOT_FOUND", "会话不存在", 404)
        return await self.history(conversation_id, turns)
