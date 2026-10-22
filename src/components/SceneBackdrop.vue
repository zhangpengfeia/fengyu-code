<script setup lang="ts">
import { artwork } from '../assets'
import { SCENE } from '../config/scene'
import OrbitTracks from './OrbitTracks.vue'
</script>

<template>
  <img class="background-art" :src="artwork.background" alt="" />
  <img
    class="earth"
    :src="artwork.earth"
    :style="{
      left: `${(SCENE.earth.left / SCENE.width) * 100}%`,
      top: `${(SCENE.earth.top / SCENE.height) * 100}%`,
      width: `${(SCENE.earth.width / SCENE.width) * 100}%`,
    }"
    alt=""
  />
  <div class="star-field" aria-hidden="true">
    <i
      v-for="n in 35"
      :key="n"
      :style="{
        left: `${(n * 73.7) % 100}%`,
        top: `${(n * 41.3) % 100}%`,
        animationDelay: `${-n * 0.3}s`,
      }"
    />
  </div>
  <OrbitTracks />
  <span
    v-for="(symbol, index) in ['✦', '★', '✦']"
    :key="index"
    class="spark"
    :class="`spark-${index + 1}`"
    aria-hidden="true"
    >{{ symbol }}</span
  >
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.background-art {
  .absolute-fill();
  width: 100%;
  height: 100%;
  z-index: -6;
}

.earth {
  position: absolute;
  height: auto;
  z-index: -3;
}

.star-field {
  .absolute-fill();
  z-index: -5;
  pointer-events: none;

  i {
    position: absolute;
    .round-control(2px);
    background: #fff28c;
    animation: twinkle 3s ease-in-out infinite;
  }
}

.spark {
  position: absolute;
  color: #ffeb69;
  text-shadow: 0 0 10px #ffe845;
  pointer-events: none;
  animation: twinkle 3s infinite;

  &-1 {
    left: 2%;
    top: 67%;
    font-size: 4cqw;
  }
  &-2 {
    right: 8%;
    top: 75%;
    font-size: 3.5cqw;
    animation-delay: -1s;
  }
  &-3 {
    left: 17%;
    top: 31%;
    font-size: 1.3cqw;
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}
</style>
