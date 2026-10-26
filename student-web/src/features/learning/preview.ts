import type { AbilityValue, EvaluationRecord, RankingEntry } from "./types";

// These fixtures are only selected by the explicit design-preview mode.
// They are never used as a fallback for a failed backend request.
export const previewAbilities: AbilityValue[] = [
  { name: "逻辑推理", value: 82 },
  { name: "空间想象", value: 75 },
  { name: "专注观察", value: 91 },
  { name: "策略规划", value: 68 },
  { name: "数感运算", value: 86 },
];

const sampleGames = [
  ["eight-pieces", "八子归位"],
  ["arrow-maze", "箭头迷宫"],
  ["number-maze", "数字迷宫"],
  ["balloon", "打气球"],
  ["stone-transfer", "石头搬运"],
  ["addition", "加法游戏"],
  ["fish", "鱼的游戏"],
  ["four-seas", "纵横四海"],
] as const;

export const previewEvaluations: EvaluationRecord[] = sampleGames.map(
  ([gameCode, title], index) => ({
    id: `preview-report-${index + 1}`,
    gameCode,
    title,
    volume: index < 5 ? 5 : 4,
    status: "COMPLETED",
    updatedAt: `2026-10-${String(26 - index).padStart(2, "0")}T08:20:00+08:00`,
    medal: index % 3 === 0 ? "GOLD" : index % 3 === 1 ? "SILVER" : "BRONZE",
    durationMs: (82 + index * 13) * 1000,
    actualSteps: 12 + index * 2,
    optimalSteps: 10 + index,
    passed: true,
    performance: `你在「${title}」中保持了很好的专注，能够观察规则并尝试不同的方法。遇到困难时，你愿意停下来思考，这是很棒的学习习惯。`,
    suggestions: [
      "开始操作前，先观察整个局面，想一想每一步可能带来的变化。",
      "试着用自己的话讲出刚才的解题思路，发现可以改进的小细节。",
      "在训练模式中尝试另一种方法，再比较两种方法的步数。",
    ],
    limitations: [
      "这是一份用于展示页面效果的示例报告，不代表真实学生的测评结果。",
    ],
    generationMode: "PREVIEW",
  }),
);

export const previewRanking: RankingEntry[] = [
  { id: "preview-101", name: "星星同学", score: 286, rank: 1 },
  { id: "preview-102", name: "小小探索家", score: 272, rank: 2 },
  { id: "preview-103", name: "元气小宇", score: 258, rank: 3 },
  { id: "preview-student", name: "杨同学", score: 246, rank: 4 },
  { id: "preview-105", name: "橙子同学", score: 238, rank: 5 },
  { id: "preview-106", name: "小鹿同学", score: 224, rank: 6 },
  { id: "preview-107", name: "追光少年", score: 216, rank: 7 },
  { id: "preview-108", name: "月亮同学", score: 205, rank: 8 },
];
