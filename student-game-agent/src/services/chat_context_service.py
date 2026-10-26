import re

from student_game_agent.backends.evaluation_repository import EvaluationRepository
from student_game_agent.backends.models import EvaluationJob
from student_game_agent.config.game_catalog import Catalog
from student_game_agent.schemas.api import AttemptData, ChatRequest, Principal, PromptSnapshot
from student_game_agent.schemas.chat_context import ChatContext
from student_game_agent.utils.errors import AppError

GAME_WORDS = (
    "游戏",
    "玩法",
    "规则",
    "怎么玩",
    "怎么走",
    "这一步",
    "下一步",
    "这关",
    "这一关",
    "操作",
    "通关",
    "棋盘",
    "获胜",
    "胜负",
    "输赢",
    "迷宫",
    "难度",
    "徽章",
)
REPORT_WORDS = (
    "徽章",
    "铜",
    "银",
    "金",
    "成绩",
    "测评",
    "报告",
    "用时",
    "步数",
    "表现",
    "通关",
    "输",
    "赢",
)
PAST_WORDS = ("刚才", "上局", "这局", "本局", "这次", "上一局", "最近")
MY_RESULT_WORDS = (
    "我的成绩",
    "我的测评",
    "我的报告",
    "我的用时",
    "我的步数",
    "我的表现",
    "我为什么",
    "我为啥",
    "我怎么会",
    "我只有",
    "我得了",
    "我拿到",
    "我获得了",
)
LEARNING_TOPIC_WORDS = ("考试", "作业", "作文", "课文", "英语", "语文", "期中", "期末")
ROUND_REFERENCE_WORDS = ("这局", "本局", "那局", "这一局", "该局", "这份报告", "这次测评", "当时")
LATEST_REPORT_WORDS = ("最新", "最近一次", "刚结束", "刚玩完", "另一局", "换一局")
FOLLOW_UPS = {
    "为什么",
    "为什么呢",
    "然后呢",
    "还有呢",
    "继续",
    "继续说",
    "解释一下",
    "展开说说",
    "那怎么办",
    "怎么改进",
    "如何提升",
    "有什么建议",
}


class ChatContextService:
    """使用明确标识、受控名称和少量规则定位上下文，不调用模型猜测成绩来源。"""

    def __init__(self, catalog: Catalog, reports: EvaluationRepository):
        self.catalog, self.reports = catalog, reports

    async def resolve(self, user: Principal, body: ChatRequest, previous: dict) -> ChatContext:
        if body.game_code:
            self.catalog.get(body.game_code)
        mentioned = self.catalog.mentioned_games(body.question)
        if len(mentioned) > 1:
            return ChatContext(
                scene="GAME_QA",
                needs_clarification=True,
                direct_answer="你想了解哪一个游戏？请先选择："
                + "、".join(g.name for g in mentioned),
            )
        game_code = mentioned[0].code if mentioned else body.game_code or previous.get("game_code")
        compact = re.sub(r"[\s，。！？!?、,.]", "", body.question)
        follow_up = compact in FOLLOW_UPS
        # 普通学习交流不能因“这次/成绩”而触发学生游戏数据查询；页面游戏只作线索。
        if (
            not body.evaluation_id
            and not mentioned
            and any(word in body.question for word in LEARNING_TOPIC_WORDS)
            and not any(word in body.question for word in ("游戏", "徽章", *ROUND_REFERENCE_WORDS))
        ):
            return ChatContext(scene="GENERAL_CHAT")
        personal_report = (
            any(word in body.question for word in PAST_WORDS)
            or any(word in body.question for word in MY_RESULT_WORDS)
            or "我的" in body.question
        ) and any(word in body.question for word in REPORT_WORDS)
        previous_report = (
            previous.get("scene") == "REPORT_QA"
            and previous.get("evaluation_id")
            and (not mentioned or mentioned[0].code == previous.get("game_code"))
            and (not body.game_code or body.game_code == previous.get("game_code"))
        )
        round_reference = any(word in body.question for word in ROUND_REFERENCE_WORDS)
        if (
            previous_report
            and round_reference
            and any(word in body.question for word in (*REPORT_WORDS, "规则", "玩法"))
        ):
            personal_report = True
        inherited_report = (
            previous_report
            and (follow_up or personal_report)
            and not any(word in body.question for word in LATEST_REPORT_WORDS)
        )
        if body.evaluation_id or personal_report or inherited_report:
            report_id = str(body.evaluation_id) if body.evaluation_id else None
            if not report_id and inherited_report:
                report_id = previous.get("evaluation_id")
            job = (
                await self.reports.student_job(user, report_id)
                if report_id
                else await self.reports.latest_student_job(user, game_code)
            )
            if not job:
                return ChatContext(
                    scene="REPORT_QA",
                    game_code=game_code,
                    direct_answer="目前还没有找到可查看的测评记录。完成一局测评后，可以在报告页查看评价。",
                )
            return self._report_context(job, mentioned[0].code if mentioned else body.game_code)
        game_question = bool(mentioned) or any(word in body.question for word in GAME_WORDS)
        if follow_up and body.game_code:
            game_question = True
        if follow_up and previous.get("scene") == "GAME_QA":
            game_question = True
        # 页面上有游戏，只是定位线索，不意味着每一句自由聊天都需要游戏规则。
        if not game_question:
            return ChatContext(scene="GENERAL_CHAT")
        if not game_code:
            return ChatContext(
                scene="GAME_QA",
                needs_clarification=True,
                direct_answer="你想了解哪一个游戏？请先选择游戏，或告诉我游戏名称。",
            )
        game = self.catalog.get(game_code)
        current_rules = any(
            word in body.question for word in ("现在", "当前", "最新", "新版")
        ) and any(word in body.question for word in ("规则", "玩法", "怎么玩"))
        version = (
            previous.get("rule_version")
            if previous.get("game_code") == game_code
            and previous.get("scene") == "GAME_QA"
            and not current_rules
            else None
        ) or game.prompt_version
        return ChatContext(scene="GAME_QA", game_code=game_code, rule_version=version)

    def _report_context(self, job: EvaluationJob, explicit_game: str | None) -> ChatContext:
        facts = AttemptData.model_validate(job.facts) if job.facts else None
        if facts and explicit_game and facts.game_code != explicit_game:
            raise AppError("REPORT_GAME_MISMATCH", "所选报告与提问中的游戏不一致，请确认报告", 409)
        context = ChatContext(
            scene="REPORT_QA",
            evaluation_id=job.id,
            game_code=facts.game_code if facts else None,
            rule_version=facts.game_rule_version if facts else None,
        )
        if job.status not in {"COMPLETED", "COMPLETED_WITH_FALLBACK"} or not job.report:
            if job.status == "FAILED":
                context.direct_answer = "这局测评的评价暂时未能完成，请稍后查看报告状态。"
            else:
                context.direct_answer = "这局测评的评价仍在生成中，请稍后查看；不需要重新提交测评。"
            return context
        if not facts:
            raise AppError("INVALID_REPORT", "报告缺少可核实的本局数据")
        game = self.catalog.get(facts.game_code)
        for value in job.prompt_snapshots or []:
            if value.get("key") == game.prompt_key:
                snapshot = PromptSnapshot.model_validate(value)
                if snapshot.version != facts.game_rule_version:
                    raise AppError("REPORT_RULE_MISMATCH", "报告保存的规则版本与本局不一致")
                context.rule_snapshot = snapshot
                break
        context.report = {
            "facts": facts.model_dump(
                mode="json", exclude={"student_id", "school_id", "source_attempt_id"}
            ),
            "evaluation": job.report.get("evaluation"),
            "generation_mode": job.report.get("generation_mode"),
        }
        return context
