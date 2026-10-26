import hashlib
import json
import time
from uuid import uuid4

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError

from src.backends.models import ChatRun, Conversation
from src.schemas.api import ChatRequest, ChatResponse, Principal
from src.utils.errors import AppError


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
        payload = body.model_dump(mode="json")
        if body.evaluation_id is None:
            payload.pop("evaluation_id")  # 保持旧版本相同请求 ID 的哈希兼容。
        payload_hash = digest(payload)
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
                return {
                    "key": key,
                    "token": token,
                    "conversation_id": conversation.id,
                    "previous_context": {
                        "game_code": conversation.game_code,
                        "rule_version": conversation.rule_version,
                        "evaluation_id": conversation.evaluation_id,
                        "scene": conversation.context_scene,
                    },
                }
        except IntegrityError as exc:
            raise AppError("REQUEST_CONFLICT", "请求已提交，请使用原请求 ID 重试", 409) from exc

    async def history(self, conversation_id: str, turns: int, context=None) -> list[dict]:
        if turns == 0:
            return []
        async with self.sessions() as session:
            query = select(ChatRun).where(
                ChatRun.conversation_id == conversation_id, ChatRun.status == "COMPLETED"
            )
            if context:
                # 避免把另一游戏/规则版本或另一局报告的回答混进当前事实上下文。
                query = query.where(ChatRun.response["scene"].as_string() == context.scene)
                if context.scene == "GAME_QA":
                    query = query.where(
                        ChatRun.response["game_code"].as_string() == context.game_code,
                        ChatRun.response["game_rule_version"].as_string() == context.rule_version,
                    )
                elif context.scene == "REPORT_QA":
                    query = query.where(
                        ChatRun.response["evaluation_id"].as_string() == context.evaluation_id
                    )
            runs = (
                await session.scalars(
                    query.order_by(ChatRun.created_at.desc(), ChatRun.id.desc()).limit(turns)
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
                .values(
                    busy_until=0,
                    busy_token="",
                    **(
                        {
                            "context_scene": response.scene,
                            "game_code": response.game_code
                            or claim["previous_context"]["game_code"],
                            "rule_version": (
                                response.game_rule_version
                                if response.game_code
                                else claim["previous_context"]["rule_version"]
                            ),
                            "evaluation_id": str(response.evaluation_id)
                            if response.evaluation_id
                            else None,
                        }
                        if response and not response.needs_clarification
                        else {}
                    ),
                )
            )

    async def owned_history(self, user: Principal, conversation_id: str, turns: int) -> list[dict]:
        async with self.sessions() as session:
            conversation = await session.get(Conversation, conversation_id)
            if not conversation or not owned(conversation, user):
                raise AppError("CONVERSATION_NOT_FOUND", "会话不存在", 404)
        return await self.history(conversation_id, turns)
