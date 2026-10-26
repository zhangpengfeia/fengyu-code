<script setup lang="ts">
import { onMounted, ref, useId } from "vue";
import { useRoute } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import { listCompanionGames } from "@/features/companion/api";
import { useCompanionChat } from "@/features/companion/useCompanionChat";
import { useCompanionPreferences } from "@/features/companion/preferences";
import type { CompanionGame } from "@/features/companion/types";
import { isPreview } from "@/shared/config";

const route = useRoute();
const props = defineProps<{ initialGame?: string; evaluation?: string }>();
const games = ref<CompanionGame[]>([]);
const gameError = ref("");
const loadingGames = ref(false);
const question = ref("");
const input = ref<HTMLTextAreaElement>();
const inputId = useId();
const { preferences } = useCompanionPreferences();
const {
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
  stop,
} = useCompanionChat();
const suggestions = [
  "我想了解游戏的玩法",
  "怎样更好地观察和思考？",
  "我想看看自己的学习报告",
];

async function loadGames() {
  loadingGames.value = true;
  gameError.value = "";
  try {
    games.value = await listCompanionGames();
  } catch (cause) {
    gameError.value =
      cause instanceof Error ? cause.message : "游戏目录暂时无法加载。";
  } finally {
    loadingGames.value = false;
  }
}

function submit() {
  const text = question.value.trim();
  if (!text || sending.value || loadingHistory.value || loadingGames.value)
    return;
  question.value = "";
  void send(text, games.value);
}

function useSuggestion(value: string) {
  question.value = value;
  input.value?.focus();
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
    event.preventDefault();
    submit();
  }
}

onMounted(async () => {
  await loadGames();
  const requested =
    props.initialGame || (typeof route.query.game === "string" ? route.query.game : "");
  const reportId =
    props.evaluation ||
    (typeof route.query.evaluation === "string" ? route.query.evaluation : "");
  await changeContext(
    games.value.some((game) => game.code === requested) ? requested : "",
    reportId,
  );
});
</script>

<template>
  <div class="chat-layout">
    <div class="chat-main">
      <div class="chat-toolbar">
        <label
          ><span>当前话题</span
          ><select
            :value="gameCode"
            :disabled="loadingGames"
            aria-label="选择聊天话题"
            @change="changeContext(($event.target as HTMLSelectElement).value)"
          >
            <option value="">学习与成长</option>
            <option v-for="game in games" :key="game.code" :value="game.code">
              {{ game.name }}
            </option>
          </select></label
        >
        <button
          class="new-chat"
          type="button"
          :disabled="!messages.length && !error"
          @click="newConversation"
        >
          <AppIcon name="refresh" :size="14" />新对话
        </button>
      </div>
      <div v-if="isPreview" class="preview-note">
        <AppIcon name="sparkles" :size="14" />设计预览 ·
        回复为示例内容，未连接真实智能服务
      </div>
      <div v-if="evaluationId" class="preview-note" role="status">
        正在讨论所选本局报告。
        <button type="button" @click="changeContext(gameCode)">切换为自由问答</button>
      </div>
      <div v-if="gameError" class="chat-alert" role="alert">
        {{ gameError
        }}<button type="button" @click="loadGames">重新连接</button>
      </div>
      <div
        ref="messageList"
        class="chat-messages"
        role="log"
        aria-live="polite"
        aria-label="与学伴的对话"
      >
        <div v-if="loadingHistory" class="history-loading">
          正在恢复本次会话…
        </div>
        <template v-else>
          <div class="chat-message assistant-message welcome-message">
            <MascotAvatar :size="34" />
            <div class="message-bubble">
              <strong>你好呀，我是{{ preferences.name }}！</strong>
              <p>
                很高兴和你一起学习。遇到有趣的问题，或者想聊聊今天的收获，都可以告诉我。
              </p>
            </div>
          </div>
          <div v-if="!messages.length" class="question-suggestions">
            <span>你可以试着问我</span
            ><button
              v-for="suggestion in suggestions"
              :key="suggestion"
              type="button"
              @click="useSuggestion(suggestion)"
            >
              {{ suggestion }}<AppIcon name="arrow-right" :size="14" />
            </button>
          </div>
          <div
            v-for="message in messages"
            :key="message.id"
            class="chat-message"
            :class="
              message.role === 'user' ? 'user-message' : 'assistant-message'
            "
          >
            <MascotAvatar v-if="message.role === 'assistant'" :size="34" />
            <div class="message-bubble">
              <p>{{ message.text }}</p>
              <div v-if="message.citations?.length" class="message-citations">
                <span
                  v-for="citation in message.citations"
                  :key="citation.source_id"
                  ><AppIcon name="book" :size="12" />{{ citation.title }}</span
                >
              </div>
            </div>
          </div>
          <div v-if="sending" class="chat-message assistant-message">
            <MascotAvatar :size="34" />
            <div class="thinking-bubble">
              <i /><i /><i /><span>{{
                isPreview ? "正在展示示例回复" : "正在认真思考"
              }}</span>
            </div>
          </div>
        </template>
      </div>
      <div v-if="error" class="chat-alert send-error" role="alert">
        <span>{{ error }}</span
        ><button
          v-if="retryPayload && !sending"
          type="button"
          @click="retry(games)"
        >
          重试本次问题
        </button>
      </div>
      <form class="chat-composer" @submit.prevent="submit">
        <label class="sr-only" :for="inputId"
          >输入想问学伴的问题</label
        >
        <textarea
          :id="inputId"
          ref="input"
          v-model="question"
          placeholder="把你的问题告诉我吧…"
          maxlength="10000"
          rows="2"
          :disabled="loadingHistory"
          @keydown="onKeydown"
        />
        <button
          v-if="sending"
          class="btn btn-soft composer-action"
          type="button"
          @click="stop"
        >
          <AppIcon name="stop" :size="17" />停止
        </button>
        <button
          v-else
          class="btn btn-primary composer-action"
          type="submit"
          :disabled="!question.trim() || loadingHistory || loadingGames"
        >
          <AppIcon name="send" :size="17" /><span>发送</span>
        </button>
        <p>回车发送 · 上档键加回车换行<span>智能建议供学习参考</span></p>
      </form>
    </div>
    <aside class="chat-sidebar">
      <div class="thinking-card">
        <div class="thinking-art">
          <MascotAvatar :size="88" /><span class="art-star">✦</span
          ><span class="art-spark">✧</span>
        </div>
        <span class="eyebrow">一点小帮助，一次大进步</span>
        <h3>让好奇心<br />带你走得更远</h3>
        <p>
          好的问题，是发现新世界的第一步。<br />慢慢想，大胆问，我会陪着你。
        </p>
      </div>
      <div class="tips-card">
        <AppIcon name="sparkles" :size="19" />
        <h4>提问小贴士</h4>
        <ul>
          <li>说说你正在玩哪一个游戏</li>
          <li>告诉我你卡在了哪个步骤</li>
          <li>也可以聊聊你的想法和发现</li>
        </ul>
      </div>
      <p class="history-note">
        “新对话”会清除当前浏览器的会话入口，已保存到服务端的记录仍会保留。
      </p>
    </aside>
  </div>
</template>

<style scoped lang="less">
.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
}
.chat-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fafbff;
}
.chat-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 24px;
  label {
    display: flex;
    gap: 10px;
    align-items: center;
    min-width: 0;
    font-size: 12px;
    color: var(--muted);
  }
  select {
    background: white;
    border: 1px solid #e1e7f6;
    padding: 7px 28px 7px 10px;
    border-radius: 8px;
    color: #52648d;
    max-width: 190px;
    font: inherit;
  }
}
.new-chat {
  display: flex;
  gap: 5px;
  align-items: center;
  background: none;
  border: 0;
  color: var(--muted);
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  &:disabled {
    opacity: 0.45;
    cursor: default;
  }
}
.preview-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 24px;
  padding: 8px 10px;
  border: 1px solid #e3e5ff;
  border-radius: 8px;
  background: #f0efff;
  color: #7777b5;
  font-size: 11px;
}
.chat-messages {
  height: min(50vh, 530px);
  min-height: 330px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 26px 24px;
  scrollbar-width: thin;
}
.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 22px;
}
.message-bubble {
  max-width: min(85%, 560px);
  padding: 13px 16px;
  background: white;
  border: 1px solid #e9edf7;
  border-radius: 0 14px 14px 14px;
  color: #4d5d7f;
  box-shadow: 0 3px 12px #293a6d03;
  font-size: 13px;
  line-height: 1.9;
  overflow-wrap: anywhere;
  p {
    white-space: pre-wrap;
    margin: 0;
  }
  strong {
    display: block;
    color: var(--ink);
    margin-bottom: 6px;
  }
}
.welcome-message .message-bubble {
  max-width: 420px;
}
.user-message {
  justify-content: flex-end;
  .message-bubble {
    background: #3863ff;
    color: white;
    border-color: #3863ff;
    border-radius: 14px 0 14px 14px;
  }
}
.question-suggestions {
  margin: -5px 0 36px 44px;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
  > span {
    font-size: 11px;
    color: #99a4bf;
    margin-bottom: 2px;
  }
  button {
    display: flex;
    align-items: center;
    gap: 18px;
    border: 1px solid #e5e9f9;
    background: white;
    padding: 8px 12px;
    color: #6b79b3;
    border-radius: 20px;
    font-size: 12px;
    text-align: left;
    cursor: pointer;
    &:hover {
      color: var(--primary);
      border-color: #b8c8ff;
    }
  }
}
.message-citations {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 10px;
  span {
    display: flex;
    gap: 4px;
    align-items: center;
    background: #f3f5ff;
    border-radius: 4px;
    padding: 2px 6px;
    font-size: 10px;
    color: #8a95b6;
  }
}
.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px;
  border: 1px solid var(--line);
  background: white;
  border-radius: 0 14px 14px;
  i {
    width: 5px;
    height: 5px;
    background: #7b94ff;
    border-radius: 50%;
    animation: thinking 1.2s infinite;
    &:nth-child(2) {
      animation-delay: 0.15s;
    }
    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
  span {
    color: #8a97b6;
    margin-left: 5px;
    font-size: 11px;
  }
}
.history-loading {
  color: var(--muted);
  text-align: center;
  padding: 50px 10px;
  font-size: 13px;
}
.chat-alert {
  padding: 12px 14px;
  margin: 12px 24px 0;
  border-radius: 8px;
  border: 1px solid #f4dfc7;
  background: #fff9ef;
  color: #986f3c;
  font-size: 12px;
  line-height: 1.7;
  button {
    border: 0;
    background: none;
    color: #b67524;
    text-decoration: underline;
    cursor: pointer;
    margin-left: 8px;
    font: inherit;
  }
}
.send-error {
  margin-top: 0;
  margin-bottom: 14px;
}
.chat-composer {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: auto;
  border-top: 1px solid var(--line);
  padding: 18px 24px 12px;
  background: white;
  textarea {
    min-width: 0;
    flex: 1;
    resize: none;
    min-height: 48px;
    border: 1px solid #e4e9f6;
    background: #f8faff;
    border-radius: 10px;
    padding: 12px 14px;
    color: var(--ink);
    font: inherit;
    font-size: 13px;
    line-height: 1.5;
    &:focus {
      outline: 2px solid #cad6ff;
      border-color: #7191ff;
    }
    &::placeholder {
      color: #a2acc3;
    }
  }
  p {
    display: flex;
    justify-content: space-between;
    margin: 0;
    color: #adb5ca;
    font-size: 10px;
    flex-basis: 100%;
  }
}
.composer-action {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.chat-sidebar {
  border-left: 1px solid var(--line);
  padding: 24px 22px;
  background: white;
}
.thinking-card {
  text-align: center;
  border-radius: 16px;
  background: linear-gradient(155deg, #f0f3ff, #f9f2ff);
  padding: 12px 12px 22px;
  .eyebrow {
    font-size: 8px;
    letter-spacing: 0.1em;
    color: #8c8ac2;
  }
  h3 {
    font-size: 23px;
    line-height: 1.6;
    color: #5365af;
    margin: 14px 0 10px;
  }
  p {
    font-size: 11px;
    color: #9b9fbb;
    line-height: 1.9;
  }
}
.thinking-art {
  height: 145px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  .art-star {
    position: absolute;
    left: 28px;
    top: 30px;
    color: #ffc765;
    font-size: 26px;
  }
  .art-spark {
    position: absolute;
    right: 26px;
    bottom: 30px;
    color: #bfb1f4;
    font-size: 24px;
  }
}
.tips-card {
  position: relative;
  color: #8b9ac2;
  padding: 23px 0 0;
  > svg {
    position: absolute;
    left: 0;
    top: 23px;
    color: #e6b75d;
  }
  h4 {
    font-size: 12px;
    color: #607095;
    padding-left: 28px;
    margin: 0 0 12px;
  }
  ul {
    list-style: none;
    padding: 0;
    margin: 0;
    font-size: 11px;
    line-height: 2.6;
  }
  li::before {
    content: "·";
    color: #9caee5;
    margin-right: 8px;
  }
}
.history-note {
  font-size: 10px;
  color: #b1b8cb;
  line-height: 1.8;
  margin-top: 24px;
}
@keyframes thinking {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .thinking-bubble i {
    animation: none;
  }
}
@media (max-width: 1000px) {
  .chat-layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .chat-sidebar {
    display: none;
  }
}
@media (max-width: 640px) {
  .chat-toolbar {
    padding: 14px;
    label {
      gap: 6px;
      font-size: 11px;
    }
    select {
      max-width: 150px;
    }
  }
  .preview-note,
  .chat-alert {
    margin-left: 14px;
    margin-right: 14px;
  }
  .chat-messages {
    padding: 20px 14px;
    height: 48dvh;
    min-height: 290px;
  }
  .message-bubble {
    font-size: 12px;
    padding: 11px 13px;
  }
  .question-suggestions {
    margin-left: 0;
    button {
      font-size: 11px;
    }
  }
  .chat-composer {
    padding: 12px 14px 10px;
    textarea {
      font-size: 16px;
    }
    p {
      font-size: 9px;
    }
  }
  .composer-action {
    padding: 12px;
    span {
      display: none;
    }
  }
}
</style>
