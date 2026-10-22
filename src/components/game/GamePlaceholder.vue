<script setup lang="ts">
import { computed } from 'vue'
import type { GameViewProps } from '../../types/game'
import LandscapeStage from '../scene/LandscapeStage.vue'

const props = withDefaults(defineProps<GameViewProps & { gameIndex: number }>(), {
  mode: 'training',
})
defineEmits<{ back: [] }>()
const game = computed(() => props.book.games[props.gameIndex])
</script>

<template>
  <LandscapeStage :style="{ background: book.theme.background }" @keydown.esc="$emit('back')">
    <section class="game-placeholder-page">
      <p>{{ book.name }} · {{ mode === 'training' ? '训练模式' : '测评模式' }}</p>
      <h1>{{ game?.title ?? '游戏准备中' }}</h1>
      <p role="status">游戏内容正在准备中</p>
      <button @click="$emit('back')">返回游戏目录</button>
    </section>
  </LandscapeStage>
</template>

<style scoped lang="less">
.game-placeholder-page {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1cqw;
  text-align: center;

  h1,
  p {
    margin: 0;
  }

  h1 {
    font-size: 4cqw;
  }

  p,
  button {
    font-size: clamp(12px, 1.6cqw, 24px);
  }

  button {
    margin-top: 2cqw;
    padding: 0.8cqw 2cqw;
    border-radius: 24px;
    background: #e8eda7;
    color: #1e3552;
  }
}
</style>
