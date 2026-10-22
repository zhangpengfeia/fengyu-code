<script setup lang="ts">
import { ref } from 'vue'
import type { Planet } from '../assets'

defineProps<{ planet: Planet }>()
const dialog = ref<HTMLDialogElement>()

function open() {
  if (!dialog.value?.open) dialog.value?.showModal()
}
function close() { dialog.value?.close() }
function onBackdropClick(event: MouseEvent) {
  if (event.target === dialog.value) close()
}

defineExpose({ open })
</script>

<template>
  <dialog
    ref="dialog"
    class="explore-dialog"
    aria-labelledby="explore-title"
    @click="onBackdropClick"
    @keydown.stop
  >
    <button class="close-dialog" aria-label="关闭" @click="close">×</button>
    <img class="dialog-planet" :src="planet.image" alt="" />
    <p class="dialog-eyebrow">AIM 星际探索</p>
    <h2 id="explore-title">{{ planet.name }}</h2>
    <p>已抵达{{ planet.name }}星球，准备开启新的探索！</p>
    <p class="demo-note">这里可接入本册目录或课程页面。</p>
    <button class="back-button" @click="close">返回星球目录</button>
  </dialog>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.explore-dialog {
  width: min(440px, 90vw);
  max-height: calc(100svh - 24px - env(safe-area-inset-top, 0px) - env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 32px;
  border: 1px solid #729fda70;
  border-radius: 26px;
  background: linear-gradient(145deg, #213c73, #101f46);
  color: @text-color;
  text-align: center;
  box-shadow: 0 30px 100px #020718aa;

  &::backdrop { background: #020a27bb; backdrop-filter: blur(8px); }
  h2 { font-size: 32px; margin: 8px; }
  p { font-size: 14px; line-height: 1.8; color: #c1d2f0; }
  .dialog-eyebrow { font-size: 12px; color: #eff2a2; letter-spacing: .2em; }
  .demo-note { color: #93a9ce; font-size: 12px; }
}

.dialog-planet { width: 140px; height: 140px; object-fit: contain; }
.close-dialog {
  position: absolute;
  right: 14px;
  top: 10px;
  background: transparent;
  font-size: 28px;
  color: #c4d4ef;
}
.back-button {
  padding: 12px 28px;
  border-radius: 30px;
  background: #e8eda7;
  color: #1e3552;
  margin-top: 12px;
  font-weight: 700;
}

@media @mobile-landscape {
  .explore-dialog {
    width: min(520px, 90vw);
    padding: 16px 24px;
    h2 { font-size: 24px; margin: 4px; }
    p { margin: 6px 0; }
  }
  .dialog-planet { width: 72px; height: 72px; }
  .back-button { margin-top: 8px; padding: 8px 24px; }
}
</style>
