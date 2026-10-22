<script setup lang="ts">
import { ref, type CSSProperties } from 'vue'
import type { BookGame } from '../../config/books'
import { gameOrbitPosition, wrapGameIndex } from '../../config/gameOrbit'
import { useOrbitDrag } from '../../composables/useOrbitDrag'
import GameCard from './GameCard.vue'

const props = defineProps<{
  games: BookGame[]
  selected: number
  layout: 'orbit' | 'grid'
  bookName: string
}>()
const emit = defineEmits<{ select: [index: number] }>()
const offset = ref(0)
const { dragging, pointerDown, pointerMove, pointerEnd, clickCapture } = useOrbitDrag({
  start: () => {
    offset.value = 0
  },
  move: (radians) => {
    // A quarter of the scene width moves one game into the front-center slot.
    offset.value += (radians / (Math.PI * 2)) * 4
  },
  end: () => {
    const next = wrapGameIndex(props.selected - Math.round(offset.value), props.games.length)
    offset.value = 0
    if (next !== props.selected) emit('select', next)
  },
})

function cardStyle(index: number): CSSProperties | undefined {
  if (props.layout === 'grid') return undefined
  const slot = gameOrbitPosition(index - props.selected + offset.value)
  return {
    left: `${slot.x}%`,
    top: `${slot.y}%`,
    width: `${slot.width}%`,
    zIndex: Math.round(slot.z),
  }
}
</script>

<template>
  <div
    class="game-list"
    :class="[layout, { dragging }]"
    role="group"
    :aria-label="`${bookName}的${games.length}个游戏${layout === 'orbit' ? '，可左右滑动' : ''}`"
    @pointerdown="layout === 'orbit' && pointerDown($event)"
    @pointermove="pointerMove"
    @pointerup="pointerEnd"
    @pointercancel="pointerEnd"
    @lostpointercapture.self="pointerEnd"
    @click.capture="clickCapture"
    @dragstart.prevent
  >
    <GameCard
      v-for="(game, index) in games"
      :key="game.id"
      :game="game"
      :index="index"
      :selected="selected === index"
      :style="cardStyle(index)"
      @select="emit('select', index)"
    />
  </div>
</template>

<style scoped lang="less">
.game-list {
  position: absolute;
  inset: 0;

  &.orbit {
    touch-action: none;
    user-select: none;
    cursor: grab;
  }

  &.dragging,
  &.dragging :deep(.game-card) {
    cursor: grabbing;
  }

  &.dragging :deep(.game-card) {
    transition: filter 0.2s;
  }

  &.grid {
    inset: 43% 10% 18%;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1cqw;
    z-index: 25;

    :deep(.game-card) {
      position: relative;
      left: auto;
      top: auto;
      width: 100%;
      transform: none;
      height: 100%;
      min-height: 0;
      aspect-ratio: auto;
    }
  }
}
</style>
