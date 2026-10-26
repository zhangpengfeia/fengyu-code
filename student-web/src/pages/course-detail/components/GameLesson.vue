<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import type { Game } from "@/features/courses/types";

defineProps<{ game: Game; index: number; busy?: boolean }>();
defineEmits<{ launch: [id: string] }>();
</script>

<template>
  <article class="game-lesson">
    <div class="lesson-number">{{ String(index + 1).padStart(2, "0") }}</div>
    <div class="lesson-content">
      <span class="lesson-eyebrow">探索关卡 {{ index + 1 }}</span>
      <h3>{{ game.name }}</h3>
      <p>{{ game.description || "带着好奇和耐心，发现属于你的解题方法。" }}</p>
    </div>
    <button
      class="lesson-button"
      :disabled="busy"
      @click="$emit('launch', game.id)"
    >
      <AppIcon name="play" :size="16" /><span>{{
        busy ? "正在准备" : "进入游戏"
      }}</span>
    </button>
  </article>
</template>

<style scoped lang="less">
.game-lesson {
  display: flex;
  align-items: center;
  gap: 19px;
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff;
}
.lesson-number {
  width: 53px;
  height: 56px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 15px;
  background: linear-gradient(140deg, #f1edff, #e3eaff);
  font-size: 22px;
  font-weight: 800;
  color: #6f7cda;
}
.lesson-eyebrow {
  font-size: 9px;
  color: #9ba6c0;
  letter-spacing: 1px;
}
.lesson-content {
  flex: 1;
  min-width: 0;
  h3 {
    font-size: 15px;
    margin: 5px 0 6px;
  }
  p {
    font-size: 11px;
    color: var(--muted);
    line-height: 1.8;
    margin: 0;
  }
}
.lesson-button {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 11px 15px;
  font: inherit;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  background: #edf2ff;
  color: var(--primary);
  border: 1px solid #e1e8ff;
  border-radius: 11px;
  cursor: pointer;
  &:hover {
    background: #e3eaff;
  }
  &:disabled {
    opacity: 0.6;
    cursor: wait;
  }
}
@media (max-width: 600px) {
  .game-lesson {
    gap: 12px;
    padding: 18px 15px;
  }
  .lesson-number {
    width: 40px;
    height: 47px;
    font-size: 18px;
    border-radius: 12px;
  }
  .lesson-content {
    h3 {
      font-size: 13px;
    }
    p {
      font-size: 10px;
    }
  }
  .lesson-button {
    padding: 11px;
    font-size: 0;
    gap: 0;
  }
}
</style>
