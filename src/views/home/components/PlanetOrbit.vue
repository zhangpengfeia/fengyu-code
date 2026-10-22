<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import type { Planet } from '../../../assets'
import PlanetCard from './PlanetCard.vue'
import { SCENE, planetPosition, planetScale } from '../../../config/scene'

const props = defineProps<{ planets: Planet[]; selected: number; angle: number }>()
defineEmits<{ select: [id: number] }>()

const orbitPlanets = computed(() =>
  props.planets.map((planet, index) => {
    const point = planetPosition(index, props.angle)
    const scale = planetScale(point.depth, planet.id === props.selected)
    const visible = scale > 0.00001
    const interactive = scale > 0.1
    const style: CSSProperties = {
      left: `${(point.x / SCENE.width) * 100}%`,
      top: `${(point.y / SCENE.height) * 100}%`,
      width: `${(point.size / SCENE.width) * 100}%`,
      zIndex: Math.round(point.depth * 30) + 1,
      visibility: visible ? 'visible' : 'hidden',
      pointerEvents: interactive ? 'auto' : 'none',
      transform: `translate(-50%, -50%) scale(${scale})`,
    }
    return { planet, track: point.track, interactive, style }
  }),
)
</script>

<template>
  <div class="planet-system" aria-label="册数星球选择">
    <PlanetCard
      v-for="{ planet, track, interactive, style } in orbitPlanets"
      :key="planet.id"
      :data-orbit="track + 1"
      :planet="planet"
      :selected="selected === planet.id"
      :style="style"
      :aria-hidden="!interactive"
      :disabled="!interactive"
      :tabindex="interactive ? 0 : -1"
      @select="$emit('select', $event)"
    />
  </div>
</template>

<style scoped lang="less">
@import (reference) '../../../styles/variables.less';

.planet-system {
  .absolute-fill();

  // RAF already interpolates position and scale; a CSS transform transition would lag behind.
  :deep(.planet) {
    transition: filter 0.3s;
  }
}
</style>
