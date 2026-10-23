<script setup lang="ts">
import type { Planet } from '../../../assets'

defineProps<{ planet: Planet; selected: boolean }>()
defineEmits<{ select: [id: number] }>()
</script>

<template>
  <button
    class="planet"
    :class="{ selected }"
    :aria-label="`选择${planet.name}`"
    :aria-pressed="selected"
    @click="$emit('select', planet.id)"
  >
    <span class="planet-glow" />
    <img
      decoding="async"
      class="planet-body"
      :src="planet.image"
      :style="{ transform: `scale(${planet.visualScale})` }"
      alt=""
      draggable="false"
    />
  </button>
</template>

<style scoped lang="less">
@import (reference) '../../../styles/variables.less';

.planet {
  position: absolute;
  aspect-ratio: 1;
  background: transparent;
  padding: 0;
  will-change: transform, left, top;
  transition:
    transform 0.65s cubic-bezier(0.2, 0.7, 0.2, 1),
    filter 0.3s;

  &:hover {
    filter: brightness(1.13);
  }

  &-body {
    width: 100%;
    height: 100%;
    object-fit: contain;
    display: block;
    filter: drop-shadow(0 8px 9px #020c3144);
  }

  &-glow {
    position: absolute;
    inset: 4%;
    border-radius: 50%;
    background: #a1f4c2;
    filter: blur(25px);
    opacity: 0;
    transition: opacity 0.6s;
  }

  &.selected {
    .planet-glow {
      opacity: 0.2;
    }
    .planet-body {
      filter: drop-shadow(0 8px 10px #05112d88);
    }
  }
}
</style>
