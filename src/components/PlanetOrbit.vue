<script setup lang="ts">
import type { CSSProperties } from 'vue'
import type { Planet } from '../assets'
import PlanetCard from './PlanetCard.vue'
import { SCENE, planetPosition, planetScale } from '../config/scene'

const props = defineProps<{ planets: Planet[]; selected: number; angle: number }>()
defineEmits<{ select: [id: number] }>()

function styleFor(index: number, planet: Planet): CSSProperties {
  const point = planetPosition(index, props.angle)
  const scale = planetScale(point.depth, planet.id === props.selected)
  return {
    left: `${(point.x / SCENE.width) * 100}%`,
    top: `${(point.y / SCENE.height) * 100}%`,
    width: `${(point.size / SCENE.width) * 100}%`,
    zIndex: Math.round(point.depth * 30) + 1,
    opacity: 0.72 + point.depth * 0.28,
    transform: `translate(-50%, -50%) scale(${scale})`,
  }
}
</script>

<template>
  <div class="planet-system" aria-label="册数星球选择">
    <PlanetCard
      v-for="(planet, index) in planets"
      :key="planet.id"
      :data-orbit="planetPosition(index, angle).track + 1"
      :planet="planet"
      :selected="selected === planet.id"
      :style="styleFor(index, planet)"
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
