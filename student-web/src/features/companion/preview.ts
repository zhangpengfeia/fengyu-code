import { previewEvaluations } from "@/features/learning/preview";
import type { CompanionGame, TutorReport } from "./types";

export const previewGames: CompanionGame[] = [
  { code: "eight-pieces", name: "八子归位" },
  { code: "arrow-maze", name: "箭头迷宫" },
  { code: "number-maze", name: "数字迷宫" },
  { code: "stone-transfer", name: "石头搬运" },
];

// Keep the tutor preview and report-detail preview on the same records.
export const previewReports: TutorReport[] = previewEvaluations
  .slice(0, 3)
  .map((item) => ({
    id: item.id,
    status: item.status,
    updated_at: item.updatedAt,
    report: {
      facts: {
        game_code: item.gameCode!,
        volume_no: item.volume!,
        badge: item.medal!,
        passed: item.passed!,
        duration_ms: item.durationMs!,
        actual_steps: item.actualSteps!,
      },
      evaluation: {
        performance: item.performance!,
        suggestions: item.suggestions,
        limitations: item.limitations,
      },
    },
  }));

export function previewAnswer(question: string, gameName?: string): string {
  return `这是一条设计预览回复，尚未调用真实智能服务。\n\n我收到了你的问题：“${question}”。${gameName ? `当前选择的是“${gameName}”。` : ""}\n\n接通后端后，我会结合已发布的游戏规则和你的提问提供帮助。现在可以继续体验发送、停止、新对话和页面切换。`;
}
