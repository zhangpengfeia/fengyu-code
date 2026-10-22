<script setup lang="ts">
import { useId } from 'vue'
import { SCENE, orbitArc, orbitPoint } from '../config/scene'

const id = useId()
const layers = [
  { name: 'back', start: Math.PI / 2, end: Math.PI * 1.5 },
  { name: 'front', start: -Math.PI / 2, end: Math.PI / 2 },
].map((layer) => ({
  ...layer,
  paths: SCENE.orbits.map((_, track) => orbitArc(layer.start, layer.end, track)),
}))
const markers = [-0.85, 0.65, -0.6].map((angle, track) => orbitPoint(angle, track))
</script>

<template>
  <svg
    v-for="layer in layers"
    :key="layer.name"
    class="orbit-tracks"
    :class="layer.name"
    :viewBox="`0 0 ${SCENE.width} ${SCENE.height}`"
    fill="none"
    aria-hidden="true"
  >
    <defs>
      <linearGradient
        :id="`${id}-${layer.name}`"
        x1="0"
        y1="675"
        x2="1200"
        y2="0"
        gradientUnits="userSpaceOnUse"
      >
        <stop stop-color="#44c6cc" />
        <stop offset=".5" stop-color="#b5df71" />
        <stop offset="1" stop-color="#8d7cff" />
      </linearGradient>
    </defs>
    <path
      v-for="(path, index) in layer.paths"
      :key="index"
      :d="path"
      :stroke="`url(#${id}-${layer.name})`"
      :stroke-width="2.5"
      :opacity="layer.name === 'front' ? 0.85 : 0.45"
    />
    <template v-if="layer.name === 'front'">
      <circle
        v-for="(point, index) in markers"
        :key="index"
        :cx="point.x"
        :cy="point.y"
        r="9"
        fill="#d5f2ff"
      />
    </template>
  </svg>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.orbit-tracks {
  .absolute-fill();
  width: 100%;
  height: 100%;
  pointer-events: none;

  &.back {
    z-index: -4;
  }
  &.front {
    z-index: -1;
  }
}
</style>
