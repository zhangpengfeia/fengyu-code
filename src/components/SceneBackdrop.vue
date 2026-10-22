<script setup lang="ts">
import { useId } from 'vue'
import { artwork } from '../assets'

defineProps<{ playing: boolean }>()
const orbitId = useId()
</script>

<template>
  <img class="background-art" :src="artwork.background" alt="" />
  <img class="earth" :src="artwork.earth" alt="" />
  <div class="star-field" :class="{ paused: !playing }" aria-hidden="true">
    <i
      v-for="n in 35"
      :key="n"
      :style="{ left: `${(n * 73.7) % 100}%`, top: `${(n * 41.3) % 100}%`, animationDelay: `${-n * .3}s` }"
    />
  </div>
  <svg class="orbits" viewBox="0 0 1200 675" fill="none" aria-hidden="true">
    <defs>
      <linearGradient :id="orbitId">
        <stop stop-color="#44c6cc" />
        <stop offset=".5" stop-color="#b5df71" />
        <stop offset="1" stop-color="#8d7cff" />
      </linearGradient>
    </defs>
    <g transform="rotate(-17 600 360)" :stroke="`url(#${orbitId})`" stroke-width="3" opacity=".75">
      <ellipse cx="600" cy="360" rx="665" ry="225" />
      <ellipse cx="600" cy="360" rx="565" ry="168" />
      <ellipse cx="600" cy="360" rx="455" ry="112" />
    </g>
    <g fill="#d5f2ff">
      <circle cx="340" cy="622" r="11" />
      <circle cx="815" cy="335" r="10" />
      <circle cx="1115" cy="380" r="11" />
    </g>
  </svg>
  <span
    v-for="(symbol, index) in ['✦', '★', '✦']"
    :key="index"
    class="spark"
    :class="[`spark-${index + 1}`, { paused: !playing }]"
    aria-hidden="true"
  >{{ symbol }}</span>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.background-art {
  .absolute-fill();
  width: 100%;
  height: 100%;
  z-index: -5;
}

.earth {
  position: absolute;
  width: 61%;
  height: 106%;
  object-fit: contain;
  left: 20%;
  top: 6.5%;
  z-index: -3;
  opacity: .85;
}

.orbits {
  .absolute-fill();
  width: 100%;
  height: 100%;
  z-index: -1;
  pointer-events: none;
}

.star-field {
  .absolute-fill();
  z-index: -2;
  pointer-events: none;

  i {
    position: absolute;
    .round-control(2px);
    background: #fff28c;
    animation: twinkle 3s ease-in-out infinite;
  }

  &.paused i { animation-play-state: paused; }
}

.spark {
  position: absolute;
  color: #ffeb69;
  text-shadow: 0 0 10px #ffe845;
  pointer-events: none;
  animation: twinkle 3s infinite;

  &.paused { animation-play-state: paused; }
  &-1 { left: 2%; top: 67%; font-size: 4cqw; }
  &-2 { right: 8%; top: 75%; font-size: 3.5cqw; animation-delay: -1s; }
  &-3 { left: 17%; top: 31%; font-size: 1.3cqw; }
}

@keyframes twinkle {
  0%, 100% { opacity: .35; }
  50% { opacity: 1; }
}

@media @mobile-portrait {
  .earth { width: 100%; height: 75%; left: 0; top: 15%; }
  .orbits { width: 170%; left: -35%; }
  .spark {
    &-1 { font-size: 7cqw; }
    &-2 { font-size: 6cqw; }
    &-3 { font-size: 4cqw; }
  }
}
</style>
