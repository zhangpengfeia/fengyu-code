from typing import TypedDict

from student_game_agent.schemas.api import ChatRequest, Citation, Principal


class ChatState(TypedDict, total=False):
    principal: Principal
    body: ChatRequest
    history: list[dict]
    snapshots: list
    documents: list
    answer: str
    citations: list[Citation]
