<script setup lang="ts">
import type { BookDefinition } from '../../config/books'
import OrbitTracks from '../scene/OrbitTracks.vue'
defineProps<{ book: BookDefinition }>()
</script>

<template>
  <div class="catalog-sky" />
  <img
    v-if="book.landscape"
    class="catalog-landscape"
    :src="book.landscape"
    alt=""
    decoding="async"
  />
  <img
    class="catalog-planet"
    :src="book.planetImage"
    :alt="`${book.name}背景星球`"
    decoding="async"
  />
  <OrbitTracks />
  <span
    v-for="(position, index) in [
      { left: '3%', top: '70%' },
      { left: '91%', top: '81%' },
      { left: '73%', top: '13%' },
    ]"
    :key="index"
    class="catalog-star"
    :style="position"
    >✦</span
  >
</template>

<style scoped lang="less">
.catalog-sky {
  position: absolute;
  inset: 0;
  z-index: -6;
  background: var(--book-background);
}
.catalog-landscape {
  position: absolute;
  width: 100%;
  left: 0;
  bottom: 0;
  opacity: 0.38;
  z-index: -5;
}
.catalog-planet {
  position: absolute;
  left: 16%;
  top: 5%;
  width: 68%;
  height: 93%;
  object-fit: contain;
  z-index: -3;
}
.catalog-star {
  position: absolute;
  color: #ffeb69;
  text-shadow: 0 0 10px #ffe845;
  font-size: 4cqw;
  pointer-events: none;
  z-index: -1;
}
</style>
