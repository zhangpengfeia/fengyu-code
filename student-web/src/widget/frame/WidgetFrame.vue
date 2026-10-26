<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import {
  WIDGET_PROTOCOL,
  isMockGameContext,
  isWidgetMessage,
  type MockGameContext,
  type WidgetMessage,
} from "@fengyu/agent-widget-sdk/protocol";
import { isMock } from "@/shared/config";
import { agentSession } from "@/shared/api/session";
import { useAuthStore } from "@/features/auth/store";
import ChatPanel from "@/features/companion/components/ChatPanel.vue";
import { openMockSession, type WidgetSession } from "./api";
const params = new URLSearchParams(window.location.search);
const parentOrigin = params.get("parent_origin") || "";
const channelId = params.get("channel_id") || "";
const allowedOrigins = (
  import.meta.env.VITE_WIDGET_PARENT_ORIGINS || window.location.origin
)
  .split(",")
  .map((value: string) => value.trim());
const permitted =
  isMock &&
  window.parent !== window &&
  allowedOrigins.includes(parentOrigin) &&
  /^[0-9a-f-]{36}$/i.test(channelId);
const auth = useAuthStore();
const context = ref<MockGameContext>();
const session = ref<WidgetSession>();
const busy = ref(false);
const error = ref("");
const scene = ref<"game" | "report">("game");
const reportStatus = ref("");
let controller: AbortController | undefined;
let revision = 0;
let renewal: ReturnType<typeof setTimeout> | undefined;
const badges: Record<string, string> = {
  NONE: "暂无徽章",
  BRONZE: "铜星",
  SILVER: "银星",
  GOLD: "金星",
};
const chatKey = computed(
  () =>
    `${context.value?.requestId}-${scene.value}-${session.value?.evaluation_id || ""}`,
);
function post(message: Omit<WidgetMessage, "protocol" | "channelId">) {
  if (permitted)
    window.parent.postMessage(
      { protocol: WIDGET_PROTOCOL, channelId, ...message },
      parentOrigin,
    );
}
async function connect(value: MockGameContext) {
  const current = ++revision;
  controller?.abort();
  clearTimeout(renewal);
  controller = new AbortController();
  context.value = value;
  session.value = undefined;
  agentSession.clear();
  error.value = "";
  busy.value = true;
  try {
    const result = await openMockSession(value, controller.signal);
    if (current !== revision) return;
    agentSession.setToken(result.access_token, result.expires_in);
    auth.user = {
      id: result.student.id,
      name: result.student.name,
      schoolId: result.student.school_id,
      mustChangePassword: false,
    };
    session.value = result;
    scene.value = "game";
    reportStatus.value = result.evaluation_id
      ? "本局示例评价正在生成，可切换到报告问答。"
      : "训练模式仅提供规则问答，不生成测评报告。";
    post({
      type: "accepted",
      requestId: value.requestId,
      evaluationId: result.evaluation_id || undefined,
    });
    // Renew using the same round ID; the backend never creates a second report.
    renewal = setTimeout(
      () => {
        if (context.value) void connect(context.value);
      },
      Math.max(30, result.expires_in - 60) * 1000,
    );
  } catch (cause) {
    if (current !== revision || controller.signal.aborted) return;
    error.value = cause instanceof Error ? cause.message : "学伴连接失败。";
    post({ type: "error", message: error.value });
  } finally {
    if (current === revision) busy.value = false;
  }
}
function receive(event: MessageEvent) {
  if (
    !permitted ||
    event.origin !== parentOrigin ||
    event.source !== window.parent ||
    !isWidgetMessage(event.data, channelId)
  )
    return;
  if (event.data.type !== "context") return;
  if (!isMockGameContext(event.data.context)) {
    error.value = "星球传入的数据格式不正确。";
    post({ type: "error", message: error.value });
    return;
  }
  void connect(event.data.context);
}
onMounted(() => {
  if (!permitted) {
    error.value = isMock
      ? "请从已配置的星球页面打开学伴。"
      : "模拟嵌入入口仅在本地模拟模式下开放。";
    return;
  }
  window.addEventListener("message", receive);
  post({ type: "ready" });
});
onBeforeUnmount(() => {
  revision++;
  controller?.abort();
  clearTimeout(renewal);
  window.removeEventListener("message", receive);
  agentSession.clear();
});
</script>
<template>
  <main class="widget-frame">
    <div v-if="busy" class="connection-state" role="status">
      <span class="spinner" />正在接收星球数据…
    </div>
    <div v-else-if="error" class="connection-state" role="alert">
      <p>{{ error }}</p>
      <button v-if="context" class="btn btn-soft" @click="connect(context)">
        重新连接
      </button>
    </div>
    <template v-else-if="session && context">
      <details class="context-receipt">
        <summary>星球已传入模拟数据 · {{ context.game.name }}</summary>
        <dl>
          <div>
            <dt>学生</dt>
            <dd>{{ session.student.name }}</dd>
          </div>
          <div>
            <dt>册次</dt>
            <dd>第{{ session.facts.volume_no }}册</dd>
          </div>
          <div>
            <dt>本局用时</dt>
            <dd>{{ Math.round(session.facts.duration_ms / 1000) }}秒</dd>
          </div>
          <div>
            <dt>操作次数</dt>
            <dd>{{ session.facts.actual_steps }}步</dd>
          </div>
          <div>
            <dt>徽章</dt>
            <dd>{{ badges[session.facts.badge] || "暂无徽章" }}</dd>
          </div>
        </dl>
      </details>
      <div class="scene-tabs" aria-label="问答场景">
        <button
          :class="{ active: scene === 'game' }"
          :aria-pressed="scene === 'game'"
          @click="scene = 'game'"
        >
          游戏规则</button
        ><button
          :class="{ active: scene === 'report' }"
          :disabled="!session.evaluation_id"
          :aria-pressed="scene === 'report'"
          @click="scene = 'report'"
        >
          本局报告
        </button>
      </div>
      <p v-if="scene === 'report'" class="report-note">{{ reportStatus }}</p>
      <ChatPanel
        :key="chatKey"
        :initial-game="context.game.code"
        :evaluation="
          scene === 'report' ? session.evaluation_id || undefined : undefined
        "
      />
    </template>
    <p v-else class="connection-state">等待星球传入数据…</p>
  </main>
</template>
<style scoped>
.widget-frame {
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: #fafbff;
  overflow: hidden;
}
.connection-state {
  padding: 30px 20px;
  margin: auto;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}
.context-receipt {
  padding: 10px 14px;
  background: #f1f5ff;
  font-size: 11px;
  color: #6c7a9c;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
  max-height: 35dvh;
  overflow: auto;
}
.context-receipt summary {
  cursor: pointer;
}
.context-receipt dl {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin: 12px 0 0;
}
.context-receipt dl > div {
  display: flex;
  gap: 6px;
}
dd {
  margin: 0;
  color: var(--ink);
}
.scene-tabs {
  display: flex;
  gap: 8px;
  padding: 8px 14px;
  background: #fff;
  flex-shrink: 0;
}
.scene-tabs button {
  border: 0;
  border-radius: 8px;
  padding: 7px 12px;
  color: var(--muted);
  background: #f4f6fc;
  font-size: 11px;
}
.scene-tabs button.active {
  color: var(--primary);
  background: #edf2ff;
}
.report-note {
  margin: 0;
  padding: 6px 14px;
  background: #fff9ec;
  color: #a27f3b;
  font-size: 10px;
  line-height: 1.6;
  flex-shrink: 0;
}
.widget-frame :deep(.chat-layout) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.widget-frame :deep(.chat-sidebar) {
  display: none;
}
.widget-frame :deep(.chat-main) {
  flex: 1;
  min-height: 0;
}
.widget-frame :deep(.chat-messages) {
  flex: 1;
  min-height: 0 !important;
  height: auto !important;
  padding: 16px 12px;
}
.widget-frame :deep(.chat-toolbar) {
  padding: 8px 12px;
}
.widget-frame :deep(.chat-composer) {
  padding: 10px 12px;
}
.widget-frame :deep(.chat-composer textarea) {
  font-size: 14px;
}
.widget-frame :deep(.message-bubble) {
  font-size: 12px;
}
</style>
