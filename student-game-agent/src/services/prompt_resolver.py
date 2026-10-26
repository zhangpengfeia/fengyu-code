import hashlib

from student_game_agent.agents.policy import LEGACY_EVALUATION_BOUNDARY
from student_game_agent.config.game_catalog import Catalog
from student_game_agent.config.nacos.prompts import NacosPromptRepository
from student_game_agent.config.settings import Settings
from student_game_agent.schemas.api import AttemptData, PromptSnapshot
from student_game_agent.schemas.prompt_context import (
    ArchivedPolicySnapshot,
    ChatScene,
    PromptBundle,
    PromptScene,
)
from student_game_agent.utils.errors import AppError


class PromptResolver:
    """按场景组装精确版本的模板；不持有用户事实，不改变 Nacos 内容。"""

    def __init__(self, settings: Settings, catalog: Catalog, prompts: NacosPromptRepository):
        self.settings, self.catalog, self.prompts = settings, catalog, prompts

    async def resolve_chat_context(
        self,
        scene: ChatScene,
        game_code: str | None = None,
        rule_version: str | None = None,
        rule_snapshot: PromptSnapshot | None = None,
    ) -> PromptBundle:
        if scene not in {"GENERAL_CHAT", "GAME_QA", "REPORT_QA"}:
            raise AppError("INVALID_CHAT_SCENE", "不支持的问答场景", 422)
        # 在第一次 await 前固定所有引用，避免一次请求混入变更后的配置。
        cfg = self.settings
        common = (cfg.base_prompt_key, cfg.base_prompt_version)
        task = (cfg.chat_prompt_key, cfg.chat_prompt_version)
        rules = None
        if scene != "GENERAL_CHAT":
            rules = self._game_reference(game_code, rule_version)
            if rule_snapshot is not None and (rule_snapshot.key, rule_snapshot.version) != rules:
                raise AppError("PROMPT_SNAPSHOT_MISMATCH", "提供的规则快照与所问游戏或版本不一致")
        elif rule_snapshot is not None:
            raise AppError("UNEXPECTED_RULE_CONTEXT", "普通聊天不应携带游戏规则", 422)
        return await self._load_bundle(scene, common, task, rules, rule_snapshot)

    async def resolve_evaluation_context(
        self,
        facts: AttemptData,
        saved_snapshots: list[dict] | None = None,
    ) -> PromptBundle:
        game = self.catalog.get(facts.game_code)
        rule_reference = (game.prompt_key, facts.game_rule_version)
        if saved_snapshots is not None:
            try:
                if len(saved_snapshots) == 2:
                    # 0.1.0 的固定顺序是 [rules, task]，只归档当时实际注入的代码策略。
                    old_rules, old_task = [
                        PromptSnapshot.model_validate(value) for value in saved_snapshots
                    ]
                    bundle = PromptBundle(
                        scene="POSTGAME_EVALUATION",
                        base=self._legacy_base(),
                        task=old_task,
                        rules=old_rules,
                    )
                else:
                    bundle = PromptBundle.from_snapshots("POSTGAME_EVALUATION", saved_snapshots)
            except (ValueError, TypeError, KeyError) as exc:
                raise AppError(
                    "INVALID_PROMPT_SNAPSHOT", "已保存的测评模板快照结构不完整或不兼容"
                ) from exc
            if (
                bundle.rules is None
                or (
                    bundle.rules.key,
                    bundle.rules.version,
                )
                != rule_reference
            ):
                raise AppError(
                    "PROMPT_SNAPSHOT_MISMATCH", "已保存的游戏规则与本局游戏或规则版本不一致"
                )
            if bundle.base.key == "legacy.code-boundary" and bundle.base != self._legacy_base():
                raise AppError("INVALID_PROMPT_SNAPSHOT", "旧版应用策略快照内容不一致")
            # 已开始的评价不重新解析当前发布版本，重试复用全部历史快照。
            return bundle
        cfg = self.settings
        common = (cfg.base_prompt_key, cfg.base_prompt_version)
        task = (cfg.evaluation_prompt_key, cfg.evaluation_prompt_version)
        return await self._load_bundle("POSTGAME_EVALUATION", common, task, rule_reference)

    @staticmethod
    def _legacy_base() -> PromptSnapshot:
        return ArchivedPolicySnapshot(
            namespace="application-code",
            key="legacy.code-boundary",
            version="0.1.0",
            template=LEGACY_EVALUATION_BOUNDARY,
            digest=hashlib.sha256(LEGACY_EVALUATION_BOUNDARY.encode()).hexdigest(),
        )

    async def prewarm_common(self) -> None:
        """预热通用与问答模板；不枚举游戏，也不加载全部规则。"""
        await self.resolve_chat_context("GENERAL_CHAT")

    def _game_reference(self, game_code: str | None, rule_version: str | None) -> tuple[str, str]:
        if not game_code:
            raise AppError("GAME_CONTEXT_REQUIRED", "请先确认要咨询的游戏", 422)
        game = self.catalog.get(game_code)
        if rule_version is not None and not rule_version.strip():
            raise AppError("INVALID_RULE_VERSION", "游戏规则版本不能为空", 422)
        return game.prompt_key, rule_version if rule_version is not None else game.prompt_version

    async def _load_bundle(
        self,
        scene: PromptScene,
        common: tuple[str, str],
        task: tuple[str, str],
        rules: tuple[str, str] | None,
        rule_snapshot: PromptSnapshot | None = None,
    ) -> PromptBundle:
        base_snapshot = await self.prompts.get(*common)
        task_snapshot = await self.prompts.get(*task)
        if rule_snapshot is None and rules:
            rule_snapshot = await self.prompts.get(*rules)
        return PromptBundle(
            scene=scene, base=base_snapshot, task=task_snapshot, rules=rule_snapshot
        )
