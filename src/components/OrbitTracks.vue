<script setup lang="ts">
import { useId } from 'vue'
import { SCENE, orbitArc, orbitPoint } from '../config/scene'

const id = useId()
const layers = [
  { name: 'back', start: Math.PI / 2, end: Math.PI * 1.5 },
  { name: 'front', start: -Math.PI / 2, end: Math.PI / 2 },
].map((layer) => ({
  ...layer,
  paths: [1.18, 1, 0.82].map((scale) => orbitArc(layer.start, layer.end, scale)),
}))
const markers = [-1.25, 0.7].map((angle) => orbitPoint(angle))
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
      :stroke-width="index === 1 ? 3 : 2"
      :opacity="index === 1 ? 0.9 : 0.45"
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
