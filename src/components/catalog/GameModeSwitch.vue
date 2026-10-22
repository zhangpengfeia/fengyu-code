<script setup lang="ts">
import type { GameMode } from '../../config/books'
defineProps<{ modelValue: GameMode }>()
defineEmits<{ 'update:modelValue': [mode: GameMode] }>()
</script>

<template>
  <div class="game-modes" role="group" aria-label="游戏模式">
    <button
      :aria-pressed="modelValue === 'training'"
      @click="$emit('update:modelValue', 'training')"
    >
      训练模式
    </button>
    <button
      :aria-pressed="modelValue === 'assessment'"
      @click="$emit('update:modelValue', 'assessment')"
    >
      测评模式
    </button>
  </div>
</template>

<style scoped lang="less">
.game-modes {
  position: absolute;
  bottom: 8%;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.8cqw;
  z-index: 35;
  button {
    padding: 0.35cqw 1.3cqw;
    border-radius: 20px;
    font-size: clamp(11px, 1.3cqw, 18px);
    background: #ffffff28;
    border: 1px solid var(--book-accent);
    white-space: nowrap;
  }
  button[aria-pressed='true'] {
    background: #fffce2;
    color: var(--book-background);
    box-shadow: 0 0 12px #ffffff30;
  }
}
</style>
