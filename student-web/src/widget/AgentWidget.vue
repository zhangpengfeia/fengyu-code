<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { useRoute } from "vue-router";
import ChatPanel from "@/features/companion/components/ChatPanel.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";

const route = useRoute();
const open = ref(false);
const dialog = ref<HTMLDialogElement>();
const game = computed(() =>
  typeof route.params.gameCode === "string" ? route.params.gameCode : "",
);
const evaluation = computed(() =>
  route.name === "report-detail" ? String(route.params.id) : "",
);
async function show() {
  open.value = true;
  await nextTick();
  dialog.value?.showModal();
}
function close() {
  dialog.value?.close();
  open.value = false;
}
</script>

<template>
  <button class="agent-launcher" type="button" aria-label="打开 AI 学伴悬浮窗" @click="show">
    <MascotAvatar :size="45" /><span>智能学伴</span>
  </button>
  <dialog
    v-if="open"
    ref="dialog"
    class="agent-dialog"
    aria-label="AI 学伴悬浮窗"
    @cancel="close"
  >
    <header>
      <strong>智能学伴</strong>
      <button type="button" aria-label="关闭学伴悬浮窗" @click="close">关闭 ×</button>
    </header>
    <ChatPanel :initial-game="game" :evaluation="evaluation" />
  </dialog>
</template>

<style scoped>
.agent-launcher {
  position: fixed;
  right: 25px;
  bottom: 32px;
  z-index: 40;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #dee4fa;
  border-radius: 18px;
  background: #fff;
  padding: 10px;
  color: #5369c8;
  box-shadow: 0 6px 25px #33466b22;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
}
.agent-dialog {
  width: min(780px, calc(100vw - 28px));
  max-height: 85dvh;
  border: 1px solid #e2e7fa;
  padding: 0;
  border-radius: 20px;
  overflow: auto;
}
.agent-dialog::backdrop {
  background: #17234066;
}
.agent-dialog header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 22px;
  border-bottom: 1px solid #e5e9f6;
  color: #445ab1;
}
.agent-dialog header button {
  border: 0;
  background: #edf1ff;
  padding: 8px 12px;
  border-radius: 8px;
  color: inherit;
  cursor: pointer;
}
@media (max-width: 767px) {
  .agent-launcher {
    right: 14px;
    bottom: calc(85px + env(safe-area-inset-bottom));
  }
}
</style>
