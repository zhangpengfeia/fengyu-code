import httpx

from src.agents.question_agent import QuestionWorkflow
from src.backends.chat_repository import ChatRepository
from src.backends.database import Database
from src.backends.evaluation_repository import EvaluationRepository
from src.config.game_catalog import Catalog
from src.config.nacos.prompts import NacosPromptRepository
from src.config.settings import Settings
from src.integrations.attempt_source import HttpAttemptSource
from src.rag.retriever import DisabledRetriever
from src.services.chat_context_service import ChatContextService
from src.services.chat_service import ChatService
from src.services.evaluation_service import EvaluationService
from src.services.prompt_resolver import PromptResolver
from src.utils.model_gateway import OpenAICompatibleGateway


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
        self.attempt_source = HttpAttemptSource(settings, self.http)
        self.mock = None
        if settings.mock_mode:
            from src.mock.providers import MockAttemptSource, MockModelGateway, MockPromptRepository
            from src.mock.runtime import MockRuntime

            self.mock = MockRuntime(settings)
            self.prompts = MockPromptRepository(settings, self.catalog, self.mock.data)
            self.model = MockModelGateway()
            self.attempt_source = MockAttemptSource(self.database, settings.integration_app_id)
        elif settings.demo_responses:
            from src.demo.providers import DemoModelGateway, DemoPromptRepository

            self.prompts = DemoPromptRepository(settings, self.catalog)
            self.model = DemoModelGateway()
        self.retriever = DisabledRetriever()
        self.prompt_resolver = PromptResolver(settings, self.catalog, self.prompts)
        self.contexts = ChatContextService(self.catalog, self.evaluation_repository)
        self.workflow = QuestionWorkflow(self.prompt_resolver, self.model, self.retriever)
        self.chat = ChatService(settings, self.chat_repository, self.workflow, self.contexts)
        self.evaluations = EvaluationService(
            settings,
            self.evaluation_repository,
            self.catalog,
            self.attempt_source,
            self.prompts,
            self.model,
            resolver=self.prompt_resolver,
        )

    async def close(self) -> None:
        await self.http.aclose()
        await self.database.engine.dispose()
