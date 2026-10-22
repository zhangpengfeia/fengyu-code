<script setup lang="ts">
import type { CSSProperties } from 'vue'
import type { Planet } from '../assets'
import PlanetCard from './PlanetCard.vue'

const props = defineProps<{ planets: Planet[]; selected: number; angle: number }>()
defineEmits<{ select: [id: number] }>()

function styleFor(index: number, id: number): CSSProperties {
  const theta = index * Math.PI / 3 + props.angle
  const depth = (Math.cos(theta) + 1) / 2
  const scale = .47 + depth * .36 + (id === props.selected ? .36 : 0)
  return {
    left: `${50 + Math.sin(theta) * 40}%`,
    top: `${48 + Math.cos(theta) * 22}%`,
    zIndex: depth < .12 ? -4 : Math.round(depth * 30) + 3,
    transform: `translate(-50%, -50%) scale(${scale})`,
  }
}
</script>

<template>
  <div class="planet-system" aria-label="册数星球选择">
    <PlanetCard
      v-for="(planet, index) in planets"
      :key="planet.id"
      :planet="planet"
      :selected="selected === planet.id"
      :style="styleFor(index, planet.id)"
      @select="$emit('select', $event)"
    />
  </div>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.planet-system { .absolute-fill(); }

@media @mobile-portrait {
  .planet-system { inset: 13% 4% 10%; }
}
</style>
