import asyncio

from src.agents.question_agent import QuestionWorkflow
from src.backends.chat_repository import ChatRepository
from src.config.settings import Settings
from src.schemas.api import ChatRequest, ChatResponse, Principal
from src.services.chat_context_service import ChatContextService
from src.utils.errors import AppError


class ChatService:
    def __init__(
        self,
        settings: Settings,
        repository: ChatRepository,
        workflow: QuestionWorkflow,
        contexts: ChatContextService,
    ):
        self.settings, self.repository, self.workflow = settings, repository, workflow
        self.contexts = contexts

    async def answer(self, user: Principal, body: ChatRequest) -> ChatResponse:
        if len(body.question) > self.settings.max_input_chars:
            raise AppError("INPUT_TOO_LONG", "问题超出允许长度", 422)
        if body.game_code:
            self.contexts.catalog.get(body.game_code)
        claim = await self.repository.begin_chat(user, body, self.settings.job_lease_seconds)
        if "response" in claim:
            return claim["response"]
        try:
            context = await self.contexts.resolve(user, body, claim["previous_context"])
            history = await self.repository.history(
                claim["conversation_id"], self.settings.history_turns, context
            )
            state = await self.workflow.graph.ainvoke(
                {
                    "principal": user,
                    "body": body,
                    "history": history,
                    "context": context,
                }
            )
            response = ChatResponse(
                request_id=body.request_id,
                conversation_id=claim["conversation_id"],
                game_code=context.game_code,
                game_rule_version=context.rule_version,
                evaluation_id=context.evaluation_id,
                scene=context.scene,
                needs_clarification=context.needs_clarification,
                answer=state["answer"],
                citations=state["citations"],
                model=self.settings.runtime_model_name if state["used_model"] else None,
            )
            await self.repository.finish_chat(
                claim, response, [p.model_dump() for p in state["snapshots"]]
            )
            return response
        except BaseException:
            # 断连/取消也释放会话；进程崩溃则由租约超时恢复。
            await asyncio.shield(self.repository.finish_chat(claim, None, []))
            raise
