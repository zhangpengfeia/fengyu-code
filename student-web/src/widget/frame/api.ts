import type { MockGameContext } from "@fengyu/agent-widget-sdk/protocol";
import { config } from "@/shared/config";
export interface MockWidgetSession {
  access_token: string;
  expires_in: number;
  evaluation_id: string | null;
  mock: true;
  student: { id: string; name: string; school_id: string };
  facts: {
    duration_ms: number;
    actual_steps: number;
    badge: string;
    volume_no: number;
    game_code: string;
  };
}
export interface LiveWidgetSession {
  access_token: string;
  expires_in: number;
  evaluation_id: null;
  mock: false;
  student: { id: string; name: string; school_id: string };
}
export type WidgetSession = MockWidgetSession | LiveWidgetSession;
export { openLiveSession } from "./live-session";

export async function openMockSession(
  context: MockGameContext,
  signal: AbortSignal,
): Promise<MockWidgetSession> {
  const response = await fetch(
    `${config.agentBaseUrl.replace(/\/$/, "")}/dev/mock/widget-session`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      signal,
      body: JSON.stringify({
        request_id: context.requestId,
        facts: {
          source_attempt_id: `planet-widget-${context.requestId}`,
          source_revision: 1,
          student_id: context.student.id,
          school_id: context.student.schoolId,
          game_code: context.game.code,
          game_rule_version: "1.0.0",
          volume_no: context.game.volume,
          mode: context.game.mode,
          difficulty: context.game.difficulty,
          duration_ms: context.result.durationMs,
          actual_steps: context.result.actualSteps,
          optimal_steps: context.result.optimalSteps,
          badge: context.result.badge,
          passed: context.result.passed,
          ended_at: new Date().toISOString(),
        },
      }),
    },
  );
  const data = await response.json().catch(() => null);
  if (!response.ok)
    throw new Error(
      data?.error?.message || data?.message || "模拟学伴暂时无法连接。",
    );
  if (!data?.mock || typeof data.access_token !== "string")
    throw new Error("当前服务未返回有效的模拟会话。");
  return data;
}
