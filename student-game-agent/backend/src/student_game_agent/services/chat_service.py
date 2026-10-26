import asyncio

from student_game_agent.agents.question_agent import QuestionWorkflow
from student_game_agent.backends.chat_repository import ChatRepository
from student_game_agent.config.settings import Settings
from student_game_agent.schemas.api import ChatRequest, ChatResponse, Principal
from student_game_agent.utils.errors import AppError


class ChatService:
    def __init__(self, settings: Settings, repository: ChatRepository, workflow: QuestionWorkflow):
        self.settings, self.repository, self.workflow = settings, repository, workflow

    async def answer(self, user: Principal, body: ChatRequest) -> ChatResponse:
        if len(body.question) > self.settings.max_input_chars:
            raise AppError("INPUT_TOO_LONG", "问题超出允许长度", 422)
        self.workflow.catalog.get(body.game_code)
        claim = await self.repository.begin_chat(user, body, self.settings.job_lease_seconds)
        if "response" in claim:
            return claim["response"]
        try:
            history = await self.repository.history(
                claim["conversation_id"], self.settings.history_turns
            )
            state = await self.workflow.graph.ainvoke(
                {
                    "principal": user,
                    "body": body,
                    "history": history,
                }
            )
            response = ChatResponse(
                request_id=body.request_id,
                conversation_id=claim["conversation_id"],
                game_code=body.game_code,
                answer=state["answer"],
                citations=state["citations"],
                model=self.settings.llm_model,
            )
            await self.repository.finish_chat(
                claim, response, [p.model_dump() for p in state["snapshots"]]
            )
            return response
        except BaseException:
            # 断连/取消也释放会话；进程崩溃则由租约超时恢复。
            await asyncio.shield(self.repository.finish_chat(claim, None, []))
            raise
