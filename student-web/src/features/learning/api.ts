import { agentRequest, educationRequest } from "@/shared/api/client";
import { isPreview } from "@/shared/config";
import { previewEvaluations, previewRanking } from "./preview";
import type {
  EvaluationRecord,
  EvaluationResponse,
  RankingEntry,
} from "./types";

const gameNames: Record<string, string> = {
  "eight-pieces": "八子归位",
  "arrow-maze": "箭头迷宫",
  "number-maze": "数字迷宫",
  balloon: "打气球",
  "stone-transfer": "石头搬运",
  addition: "加法游戏",
  fish: "鱼的游戏",
  "four-seas": "纵横四海",
};

function toEvaluation(raw: EvaluationResponse): EvaluationRecord {
  const facts = raw.report?.facts;
  const evaluation = raw.report?.evaluation;
  return {
    id: raw.id,
    status: raw.status,
    updatedAt: raw.updated_at,
    title: facts ? (gameNames[facts.game_code] ?? "游戏测评") : "游戏测评",
    gameCode: facts?.game_code,
    volume: facts?.volume_no,
    medal: facts?.badge,
    durationMs: facts?.duration_ms,
    actualSteps: facts?.actual_steps,
    optimalSteps: facts?.optimal_steps,
    passed: facts?.passed,
    performance: evaluation?.performance,
    suggestions: evaluation?.suggestions ?? [],
    limitations: evaluation?.limitations ?? [],
    generationMode: raw.report?.generation_mode,
    errorCode: raw.error_code,
  };
}

export async function listEvaluations(
  limit = 20,
  offset = 0,
): Promise<EvaluationRecord[]> {
  if (isPreview) return previewEvaluations.slice(offset, offset + limit);
  const rows = await agentRequest<EvaluationResponse[]>({
    url: "/api/v1/evaluations",
    params: { limit, offset },
  });
  return rows.map(toEvaluation);
}

export async function getEvaluation(id: string): Promise<EvaluationRecord> {
  if (isPreview) {
    const record = previewEvaluations.find((item) => item.id === id);
    if (!record)
      throw new Error("没有找到这份示例报告，请返回报告列表重新选择。");
    return record;
  }
  const raw = await agentRequest<EvaluationResponse>({
    url: `/api/v1/evaluations/${encodeURIComponent(id)}`,
  });
  return toEvaluation(raw);
}

export async function getRanking(params: {
  classId?: string;
  volume?: number;
}): Promise<RankingEntry[]> {
  if (isPreview) {
    return previewRanking.map((item) => ({
      ...item,
      score: params.volume
        ? Math.round(item.score * (params.volume === 5 ? 0.62 : 0.38))
        : item.score,
    }));
  }
  if (!params.classId)
    throw new Error(
      "暂未获取到你的班级信息，请刷新页面或联系老师确认班级归属。",
    );
  const rows = await educationRequest<
    {
      user_id: string | number;
      nick_name: string;
      total_score: number | string;
    }[]
  >({
    url: "/biz/learning/ranking",
    params: { class_id: params.classId, volume: params.volume },
  });
  return rows.map((row, index) => ({
    id: String(row.user_id),
    name: row.nick_name || "同学",
    score: Number(row.total_score),
    rank: index + 1,
  }));
}

export function formatDuration(milliseconds?: number): string {
  if (milliseconds === undefined) return "—";
  const seconds = Math.floor(milliseconds / 1000);
  return seconds < 60
    ? `${seconds} 秒`
    : `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`;
}

export function formatReportDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "日期待确认";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "long",
    day: "numeric",
    timeZone: "Asia/Shanghai",
  }).format(date);
}
