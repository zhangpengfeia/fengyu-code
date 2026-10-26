from typing import TypedDict

from src.schemas.api import ChatRequest, Citation, Principal
from src.schemas.chat_context import ChatContext
from src.schemas.prompt_context import PromptBundle


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
