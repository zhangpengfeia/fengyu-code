import asyncio
from types import SimpleNamespace

import pytest
from pydantic import ValidationError

from student_game_agent.agents.policy import LEGACY_EVALUATION_BOUNDARY
from student_game_agent.config.game_catalog import Catalog, Game
from student_game_agent.schemas.api import AttemptData, PromptSnapshot
from student_game_agent.schemas.prompt_context import PromptBundle
from student_game_agent.services.prompt_resolver import PromptResolver
from student_game_agent.utils.errors import AppError


def settings():
    return SimpleNamespace(
        base_prompt_key="companion.base",
        base_prompt_version="1.0.0",
        chat_prompt_key="companion.chat",
        chat_prompt_version="1.0.0",
        evaluation_prompt_key="companion.evaluation",
        evaluation_prompt_version="1.0.0",
    )


def snapshot(key, version):
    return PromptSnapshot(
        namespace="test", key=key, version=version, template=f"{key} {version}", digest="test"
    )


class Prompts:
    def __init__(self, on_first_get=None):
        self.calls = []
        self.on_first_get = on_first_get

    async def get(self, key, version):
        self.calls.append((key, version))
        if len(self.calls) == 1 and self.on_first_get:
            self.on_first_get()
        return snapshot(key, version)


def catalog():
    return Catalog(
        [
            Game(
                code="eight-pieces",
                name="八子归位",
                prompt_key="game.rules.eight-pieces",
                prompt_version="2.0.0",
            )
        ]
    )


def facts():
    return AttemptData(
        source_attempt_id="attempt-1",
        source_revision=1,
        student_id="student-1",
        school_id="school-1",
        game_code="eight-pieces",
        game_rule_version="1.0.0",
        volume_no=5,
        mode="ASSESSMENT",
        difficulty="NOVICE",
        duration_ms=83000,
        actual_steps=36,
        optimal_steps=28,
        badge="BRONZE",
        passed=True,
        ended_at="2026-10-26T02:00:00Z",
    )


def test_general_chat_and_prewarm_do_not_resolve_games():
    async def scenario():
        class UnusedCatalog:
            def get(self, code):
                raise AssertionError("普通聊天不应查询游戏目录")

        prompts = Prompts()
        resolver = PromptResolver(settings(), UnusedCatalog(), prompts)
        bundle = await resolver.resolve_chat_context("GENERAL_CHAT", game_code="irrelevant")
        assert bundle.rules is None
        assert prompts.calls == [("companion.base", "1.0.0"), ("companion.chat", "1.0.0")]
        prompts.calls.clear()
        await resolver.prewarm_common()
        assert len(prompts.calls) == 2

    asyncio.run(scenario())


def test_game_question_uses_catalog_version_and_report_uses_explicit_version():
    async def scenario():
        prompts = Prompts()
        resolver = PromptResolver(settings(), catalog(), prompts)
        game = await resolver.resolve_chat_context("GAME_QA", "eight-pieces")
        report = await resolver.resolve_chat_context("REPORT_QA", "eight-pieces", "1.0.0")
        assert game.rules.version == "2.0.0"
        assert report.rules.version == "1.0.0"
        assert len(game.snapshots) == 3

    asyncio.run(scenario())


def test_game_context_and_version_errors_precede_nacos_access():
    async def scenario():
        prompts = Prompts()
        resolver = PromptResolver(settings(), catalog(), prompts)
        with pytest.raises(AppError, match="GAME_CONTEXT_REQUIRED"):
            await resolver.resolve_chat_context("GAME_QA")
        with pytest.raises(AppError, match="INVALID_RULE_VERSION"):
            await resolver.resolve_chat_context("GAME_QA", "eight-pieces", "")
        assert prompts.calls == []

    asyncio.run(scenario())


def test_evaluation_uses_actual_rule_version_and_reuses_all_saved_snapshots():
    async def scenario():
        cfg, prompts = settings(), Prompts()
        resolver = PromptResolver(cfg, catalog(), prompts)
        bundle = await resolver.resolve_evaluation_context(facts())
        assert bundle.scene == "POSTGAME_EVALUATION"
        assert bundle.rules.version == "1.0.0"
        assert prompts.calls == [
            ("companion.base", "1.0.0"),
            ("companion.evaluation", "1.0.0"),
            ("game.rules.eight-pieces", "1.0.0"),
        ]
        cfg.base_prompt_version = cfg.evaluation_prompt_version = "3.0.0"
        prompts.calls.clear()
        retried = await resolver.resolve_evaluation_context(facts(), bundle.to_snapshots())
        assert retried == bundle
        assert prompts.calls == []

    asyncio.run(scenario())


@pytest.mark.parametrize("field,value", [("key", "game.rules.other"), ("version", "2.0.0")])
def test_mismatched_saved_rules_are_rejected(field, value):
    async def scenario():
        prompts = Prompts()
        resolver = PromptResolver(settings(), catalog(), prompts)
        bundle = await resolver.resolve_evaluation_context(facts())
        saved = bundle.to_snapshots()
        saved[2][field] = value
        prompts.calls.clear()
        with pytest.raises(AppError, match="PROMPT_SNAPSHOT_MISMATCH"):
            await resolver.resolve_evaluation_context(facts(), saved)
        assert prompts.calls == []

    asyncio.run(scenario())


def test_incomplete_saved_evaluation_is_not_silently_replaced():
    async def scenario():
        prompts = Prompts()
        resolver = PromptResolver(settings(), catalog(), prompts)
        with pytest.raises(AppError, match="INVALID_PROMPT_SNAPSHOT"):
            await resolver.resolve_evaluation_context(facts(), [])
        assert prompts.calls == []

    asyncio.run(scenario())


def test_request_captures_all_configuration_references_before_first_await():
    async def scenario():
        cfg, games = settings(), catalog()

        def update_configuration():
            cfg.chat_prompt_version = "3.0.0"
            games.get("eight-pieces").prompt_version = "3.0.0"

        prompts = Prompts(update_configuration)
        bundle = await PromptResolver(cfg, games, prompts).resolve_chat_context(
            "GAME_QA", "eight-pieces"
        )
        assert bundle.task.version == "1.0.0"
        assert bundle.rules.version == "2.0.0"

    asyncio.run(scenario())


def test_legacy_evaluation_retries_reuse_original_code_policy_and_nacos_snapshots():
    async def scenario():
        prompts = Prompts()
        cfg = settings()
        cfg.base_prompt_version = cfg.evaluation_prompt_version = "9.0.0"
        resolver = PromptResolver(cfg, catalog(), prompts)
        legacy = [
            snapshot("game.rules.eight-pieces", "1.0.0").model_dump(),
            snapshot("companion.evaluation", "0.1.0").model_dump(),
        ]
        bundle = await resolver.resolve_evaluation_context(facts(), legacy)
        assert bundle.base.key == "legacy.code-boundary"
        assert bundle.base.template == LEGACY_EVALUATION_BOUNDARY
        assert bundle.task.version == "0.1.0"
        assert bundle.rules.version == "1.0.0"
        assert prompts.calls == []
        # 归档为新结构后仍能识别代码策略，不读取当下发布版本。
        assert await resolver.resolve_evaluation_context(facts(), bundle.to_snapshots()) == bundle
        assert prompts.calls == []

    asyncio.run(scenario())


def test_report_question_reuses_saved_rule_without_nacos_rule_lookup():
    async def scenario():
        prompts = Prompts()
        resolver = PromptResolver(settings(), catalog(), prompts)
        saved = snapshot("game.rules.eight-pieces", "1.0.0")
        bundle = await resolver.resolve_chat_context(
            "REPORT_QA", "eight-pieces", "1.0.0", rule_snapshot=saved
        )
        assert bundle.rules == saved
        assert prompts.calls == [("companion.base", "1.0.0"), ("companion.chat", "1.0.0")]
        with pytest.raises(AppError, match="PROMPT_SNAPSHOT_MISMATCH"):
            await resolver.resolve_chat_context(
                "REPORT_QA", "eight-pieces", "2.0.0", rule_snapshot=saved
            )
        with pytest.raises(AppError, match="UNEXPECTED_RULE_CONTEXT"):
            await resolver.resolve_chat_context("GENERAL_CHAT", rule_snapshot=saved)

    asyncio.run(scenario())


def test_bundle_round_trip_and_composition_validation():
    bundle = PromptBundle(
        scene="GENERAL_CHAT", base=snapshot("base", "1"), task=snapshot("chat", "1")
    )
    assert PromptBundle.from_snapshots("GENERAL_CHAT", bundle.to_snapshots()) == bundle
    with pytest.raises(ValidationError):
        bundle.scene = "GAME_QA"
    with pytest.raises(ValidationError, match="必须绑定"):
        PromptBundle(scene="GAME_QA", base=bundle.base, task=bundle.task)
    with pytest.raises(ValueError, match="必须包含"):
        PromptBundle.from_snapshots("POSTGAME_EVALUATION", bundle.to_snapshots())
