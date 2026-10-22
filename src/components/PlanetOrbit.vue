<script setup lang="ts">
import type { CSSProperties } from 'vue'
import type { Planet } from '../assets'
import PlanetCard from './PlanetCard.vue'
import { SCENE, orbitPoint } from '../config/scene'

const props = defineProps<{ planets: Planet[]; selected: number; angle: number }>()
defineEmits<{ select: [id: number] }>()

function styleFor(index: number, id: number): CSSProperties {
  const theta = (index * Math.PI) / 3 + props.angle
  const point = orbitPoint(theta)
  const scale = 0.5 + point.depth * 0.3 + (id === props.selected ? 0.52 : 0)
  return {
    left: `${(point.x / SCENE.width) * 100}%`,
    top: `${(point.y / SCENE.height) * 100}%`,
    width: `${(SCENE.planetSize / SCENE.width) * 100}%`,
    zIndex: point.behindEarth ? -4 : Math.round(point.depth * 30) + 3,
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

.planet-system {
  .absolute-fill();
}
</style>
