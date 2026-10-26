from typing import Protocol

from student_game_agent.rag.schemas import RetrievalQuery, RetrievedDocument


class Retriever(Protocol):
    """后续实现向量/多模态检索；实现方必须按身份、游戏和规则版本过滤。"""

    async def retrieve(self, query: RetrievalQuery) -> list[RetrievedDocument]: ...


class DisabledRetriever:
    async def retrieve(self, query: RetrievalQuery) -> list[RetrievedDocument]:
        return []
