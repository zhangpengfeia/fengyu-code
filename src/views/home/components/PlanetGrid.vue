<script setup lang="ts">
import type { Planet } from '../../../assets'
import PlanetCard from './PlanetCard.vue'

defineProps<{ planets: Planet[]; selected: number }>()
defineEmits<{ select: [id: number] }>()
</script>

<template>
  <div class="planet-grid" role="group" aria-label="全部12册星球">
    <PlanetCard
      v-for="planet in planets"
      :key="planet.id"
      :planet="planet"
      :selected="selected === planet.id"
      @select="$emit('select', $event)"
    />
  </div>
</template>

<style scoped lang="less">
.planet-grid {
  position: absolute;
  inset: 31% 12% 22%;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  place-items: center;
  gap: 1cqw;
  z-index: 32;
  border: 1px solid #a0c8f71c;
  border-radius: 2cqw;
  background: #0a1d4066;
  padding: 1cqw;

  :deep(.planet) {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 0;
    aspect-ratio: auto;
    transform: scale(0.88);
    transition:
      transform 0.25s,
      filter 0.3s;
    will-change: auto;

    &.selected {
      transform: scale(1);
    }
    .planet-glow {
      inset: 10%;
    }
  }
}
</style>
