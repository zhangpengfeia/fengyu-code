import { agentRequest, ensureAgentToken } from "@/shared/api/client";
import { agentSession } from "@/shared/api/session";
import { config, isPreview } from "@/shared/config";
import { previewGames, previewReports } from "./preview";
import { readEventStream } from "./sse";
import type {
  ChatAnswer,
  ChatPayload,
  CompanionGame,
  TutorReport,
} from "./types";

export async function listCompanionGames(): Promise<CompanionGame[]> {
  if (isPreview) return previewGames;
  const result = await agentRequest<{ games: CompanionGame[] }>({
    url: "/api/v1/games",
  });
  return result.games;
}

export async function getConversation(id: string) {
  return agentRequest<{ turns: { question: string; answer: string }[] }>({
    url: `/api/v1/conversations/${encodeURIComponent(id)}/messages`,
    params: { limit: 30 },
  });
}

export async function listTutorReports(): Promise<TutorReport[]> {
  if (isPreview) return previewReports;
  return agentRequest<TutorReport[]>({
    url: "/api/v1/evaluations",
    params: { limit: 10, offset: 0 },
  });
}

export async function askCompanion(
  payload: ChatPayload,
  signal: AbortSignal,
  onAnswer: (answer: ChatAnswer) => void,
) {
  const token = await ensureAgentToken();
  signal.throwIfAborted();
  const response = await fetch(
    `${config.agentBaseUrl.replace(/\/$/, "")}/api/v1/chat/stream`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
      signal,
    },
  );
  if (!response.ok) {
    if (response.status === 401 && agentSession.getToken() === token)
      agentSession.clear();
    const body = await response.json().catch(() => null);
    const detail =
      typeof body?.detail === "string" ? body.detail : body?.detail?.message;
    throw new Error(
      body?.error?.message ||
        body?.message ||
        detail ||
        `学伴暂时无法回答（${response.status}），请稍后重试。`,
    );
  }
  if (!response.headers.get("content-type")?.includes("text/event-stream")) {
    throw new Error("学伴返回了非预期的消息格式，请检查服务连接设置。");
  }
  let received = false;
  await readEventStream(response, ({ event, data }) => {
    if (event !== "answer" && event !== "error") return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(data);
    } catch {
      throw new Error("学伴消息未能完整解析，请重试。");
    }
    if (event === "error") {
      const error = parsed as { message?: string };
      throw new Error(error.message || "学伴处理失败，请稍后重试。");
    }
    const answer = parsed as ChatAnswer;
    if (
      typeof answer.answer !== "string" ||
      typeof answer.conversation_id !== "string"
    ) {
      throw new Error("学伴回复缺少必要内容，请重试。");
    }
    received = true;
    onAnswer(answer);
  });
  if (!received)
    throw new Error("连接已结束，但没有收到回答。请重试本次问题。");
}
