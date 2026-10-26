from typing import TypedDict

from student_game_agent.schemas.api import ChatRequest, Citation, Principal
from student_game_agent.schemas.chat_context import ChatContext
from student_game_agent.schemas.prompt_context import PromptBundle


class ChatState(TypedDict, total=False):
    principal: Principal
    body: ChatRequest
    context: ChatContext
    prompt_bundle: PromptBundle
    history: list[dict]
    snapshots: list
    documents: list
    answer: str
    citations: list[Citation]
    used_model: bool
