import httpx

from student_game_agent.agents.question_agent import QuestionWorkflow
from student_game_agent.backends.chat_repository import ChatRepository
from student_game_agent.backends.database import Database
from student_game_agent.backends.evaluation_repository import EvaluationRepository
from student_game_agent.config.game_catalog import Catalog
from student_game_agent.config.nacos.prompts import NacosPromptRepository
from student_game_agent.config.settings import Settings
from student_game_agent.integrations.attempt_source import HttpAttemptSource
from student_game_agent.rag.retriever import DisabledRetriever
from student_game_agent.services.chat_service import ChatService
from student_game_agent.services.evaluation_service import EvaluationService
from student_game_agent.utils.model_gateway import OpenAICompatibleGateway


class Container:
    """单一装配入口；RAG 或数据源扩展时替换对应适配器即可。"""

    def __init__(self, settings: Settings, http_client: httpx.AsyncClient | None = None):
        self.settings = settings
        self.database = Database(settings.database_url)
        self.chat_repository = ChatRepository(self.database)
        self.evaluation_repository = EvaluationRepository(self.database)
        self.catalog = Catalog.load(settings.catalog_path)
        self.http = http_client or httpx.AsyncClient(
            timeout=10, follow_redirects=False, trust_env=False
        )
        self.prompts = NacosPromptRepository(settings, self.http)
        self.model = OpenAICompatibleGateway(settings, self.http)
        self.retriever = DisabledRetriever()
        self.workflow = QuestionWorkflow(
            settings, self.catalog, self.prompts, self.model, self.retriever
        )
        self.chat = ChatService(settings, self.chat_repository, self.workflow)
        self.evaluations = EvaluationService(
            settings,
            self.evaluation_repository,
            self.catalog,
            HttpAttemptSource(settings, self.http),
            self.prompts,
            self.model,
        )

    async def close(self) -> None:
        await self.http.aclose()
        await self.database.engine.dispose()
