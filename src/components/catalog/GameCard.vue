<script setup lang="ts">
import type { BookGame } from '../../config/books'
defineProps<{ game: BookGame; index: number; selected: boolean }>()
defineEmits<{ select: [] }>()
</script>

<template>
  <button
    class="game-card"
    :class="{ selected }"
    :aria-label="`选择${game.title}`"
    :aria-pressed="selected"
    @click="$emit('select')"
  >
    <img v-if="game.thumbnail" :src="game.thumbnail" alt="" decoding="async" draggable="false" />
    <span v-else class="game-placeholder"
      ><span>✦</span><strong>{{ String(index + 1).padStart(2, '0') }}</strong
      ><small>游戏准备中</small></span
    >
    <span class="game-caption">{{ String(index + 1).padStart(2, '0') }} · {{ game.title }}</span>
  </button>
</template>

<style scoped lang="less">
.game-card {
  padding: 0;
  background: transparent;
  position: absolute;
  aspect-ratio: 1.53;
  transform: translate(-50%, -50%);
  transition:
    left 0.45s ease,
    top 0.45s ease,
    width 0.45s ease,
    filter 0.2s;
  border-radius: 12%;
  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
  }
  &:hover {
    filter: brightness(1.1);
  }
  &.selected {
    filter: drop-shadow(0 0 12px var(--book-glow));
  }
}
.game-caption {
  position: absolute;
  bottom: 5%;
  left: 10%;
  right: 10%;
  background: #07172bc9;
  padding: 0.2cqw 0.4cqw;
  border-radius: 2cqw;
  color: white;
  font-size: clamp(7px, 0.9cqw, 14px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.game-placeholder {
  position: absolute;
  inset: 0;
  border: 0.5cqw solid var(--book-accent);
  border-radius: 12%;
  background: linear-gradient(145deg, #ffffff20, #07172bd9);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6%;
  color: var(--book-accent);
  > span {
    font-size: 4cqw;
  }
  strong {
    font-size: 3cqw;
  }
  small {
    position: absolute;
    bottom: 28%;
    font-size: 1cqw;
  }
}
</style>
