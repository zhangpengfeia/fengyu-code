import { computed, nextTick, onBeforeUnmount, ref } from "vue";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import { askCompanion, getConversation } from "./api";
import { previewAnswer } from "./preview";
import type { ChatMessage, ChatPayload, CompanionGame } from "./types";

function waitForPreview(signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const abort = () => {
      clearTimeout(timer);
      reject(new DOMException("Aborted", "AbortError"));
    };
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", abort);
      resolve();
    }, 450);
    signal.addEventListener("abort", abort, { once: true });
    if (signal.aborted) abort();
  });
}

export function useCompanionChat() {
  const auth = useAuthStore();
  const gameCode = ref("");
  const evaluationId = ref("");
  const messages = ref<ChatMessage[]>([]);
  const sending = ref(false);
  const loadingHistory = ref(false);
  const error = ref("");
  const conversationId = ref("");
  const messageList = ref<HTMLElement>();
  const retryPayload = ref<ChatPayload>();
  let controller: AbortController | undefined;
  let contextVersion = 0;

  const historyKey = computed(
    () =>
      `fengyu.companion.${auth.user?.id || "guest"}.${gameCode.value || "general"}.${evaluationId.value || "chat"}`,
  );

  async function scrollToLatest() {
    await nextTick();
    messageList.value?.scrollTo({
      top: messageList.value.scrollHeight,
      behavior: "smooth",
    });
  }

  async function changeContext(value: string, reportId = "") {
    contextVersion++;
    const version = contextVersion;
    controller?.abort();
    sending.value = false;
    loadingHistory.value = false;
    retryPayload.value = undefined;
    gameCode.value = value;
    evaluationId.value = reportId;
    messages.value = [];
    error.value = "";
    conversationId.value = "";
    if (isPreview) return;
    try {
      const id = sessionStorage.getItem(historyKey.value);
      if (!id) return;
      loadingHistory.value = true;
      const history = await getConversation(id);
      if (version !== contextVersion) return;
      conversationId.value = id;
      messages.value = history.turns.flatMap((turn, index) => [
        {
          id: `history-${index}-user`,
          role: "user" as const,
          text: turn.question,
        },
        {
          id: `history-${index}-assistant`,
          role: "assistant" as const,
          text: turn.answer,
        },
      ]);
      void scrollToLatest();
    } catch (cause) {
      if (version === contextVersion)
        error.value =
          cause instanceof Error ? cause.message : "暂时无法恢复对话。";
    } finally {
      if (version === contextVersion) loadingHistory.value = false;
    }
  }

  async function run(payload: ChatPayload, gameName?: string) {
    const version = contextVersion;
    controller = new AbortController();
    const signal = controller.signal;
    sending.value = true;
    error.value = "";
    retryPayload.value = undefined;
    let answered = false;
    try {
      if (isPreview) {
        await waitForPreview(signal);
        if (version !== contextVersion) return;
        messages.value.push({
          id: crypto.randomUUID(),
          role: "assistant",
          text: previewAnswer(payload.question, gameName),
          preview: true,
        });
        answered = true;
      } else {
        await askCompanion(payload, signal, (answer) => {
          if (version !== contextVersion || signal.aborted) return;
          answered = true;
          if (answer.game_code && !answer.needs_clarification) gameCode.value = answer.game_code;
          conversationId.value = answer.conversation_id;
          try {
            sessionStorage.setItem(historyKey.value, answer.conversation_id);
          } catch {
            /* Chat still works without history storage. */
          }
          messages.value.push({
            id: answer.request_id,
            role: "assistant",
            text: answer.answer,
            citations: answer.citations,
          });
          void scrollToLatest();
        });
      }
      void scrollToLatest();
    } catch (cause) {
      if (version !== contextVersion) return;
      error.value = signal.aborted
        ? "已停止等待。服务端可能仍在处理，可稍后重试本次问题。"
        : cause instanceof Error
          ? cause.message
          : "暂时无法连接学伴，请重试。";
      if (!answered) retryPayload.value = payload;
    } finally {
      if (version === contextVersion) sending.value = false;
    }
  }

  async function send(question: string, games: CompanionGame[]) {
    const text = question.trim();
    if (!text || sending.value || loadingHistory.value) return;
    const payload: ChatPayload = {
      request_id: crypto.randomUUID(),
      question: text,
      ...(gameCode.value ? { game_code: gameCode.value } : {}),
      ...(evaluationId.value ? { evaluation_id: evaluationId.value } : {}),
      ...(conversationId.value
        ? { conversation_id: conversationId.value }
        : {}),
    };
    messages.value.push({
      id: `${payload.request_id}-user`,
      role: "user",
      text,
    });
    void scrollToLatest();
    await run(
      payload,
      games.find((game) => game.code === gameCode.value)?.name,
    );
  }

  function retry(games: CompanionGame[]) {
    if (retryPayload.value && !sending.value) {
      return run(
        retryPayload.value,
        games.find((game) => game.code === gameCode.value)?.name,
      );
    }
  }

  function newConversation() {
    try {
      sessionStorage.removeItem(historyKey.value);
    } catch {
      /* Storage can be unavailable in private mode. */
    }
    contextVersion++;
    controller?.abort();
    messages.value = [];
    conversationId.value = "";
    retryPayload.value = undefined;
    error.value = "";
    sending.value = false;
    loadingHistory.value = false;
  }

  onBeforeUnmount(() => {
    contextVersion++;
    controller?.abort();
  });
  return {
    gameCode,
    evaluationId,
    messages,
    sending,
    loadingHistory,
    error,
    messageList,
    retryPayload,
    changeContext,
    send,
    retry,
    newConversation,
    stop: () => controller?.abort(),
  };
}
