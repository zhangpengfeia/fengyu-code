<script setup lang="ts">
import { artwork } from '../assets'

defineProps<{ page: number; playing: boolean }>()
defineEmits<{ toggleMotion: [] }>()
</script>

<template>
  <header class="profile">
    <img :src="artwork.profile" alt="杨同学，大师" />
  </header>
  <div class="top-actions">
    <span class="chapter">星球目录 · {{ page === 0 ? '01 — 06' : '07 — 12' }}</span>
    <button
      class="motion-button"
      :aria-label="playing ? '暂停动画' : '播放动画'"
      :aria-pressed="!playing"
      @click="$emit('toggleMotion')"
    >{{ playing ? 'Ⅱ' : '▷' }}</button>
  </div>
  <img class="title-art" :src="artwork.title" alt="AIM趣智 · 数创星球" />
  <p class="subtitle">点击一颗星球，开启你的奇妙探索</p>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.profile {
  position: absolute;
  top: 4%;
  left: 2%;
  width: 19%;
  z-index: 40;

  img { width: 100%; display: block; }
}

.top-actions {
  position: absolute;
  right: 3%;
  top: 5%;
  display: flex;
  align-items: center;
  gap: 14px;
  z-index: 40;
}

.chapter {
  font-size: 1cqw;
  color: @muted-color;
  letter-spacing: .12em;
}

.motion-button {
  .round-control(3cqw);
  min-width: 28px;
  min-height: 28px;
  border: 1px solid #ffffff38;
  background: #183469a6;
  font-size: 1.5cqw;
  display: grid;
  place-items: center;
}

.title-art {
  position: absolute;
  top: 23%;
  left: 24%;
  width: 52%;
  z-index: 38;
}

.subtitle {
  position: absolute;
  top: 38%;
  width: 100%;
  margin: 0;
  text-align: center;
  letter-spacing: .22em;
  font-size: 1.1cqw;
  color: #d3e4ff;
  z-index: 38;
  text-shadow: 0 2px 6px #14254d;
}

@media @mobile-portrait {
  .profile { width: 32%; top: 3%; left: 4%; }
  .top-actions { top: 4%; right: 4%; }
  .chapter { font-size: 2cqw; }
  .motion-button { .round-control(7cqw); font-size: 4cqw; }
  .title-art { width: 80%; left: 10%; top: 19%; }
  .subtitle { top: 31.5%; font-size: 2.2cqw; }
}

@media @mobile-landscape {
  .top-actions { top: 4%; gap: 8px; }
  .chapter { font-size: clamp(10px, 1.5cqw, 13px); }
  .motion-button { .round-control(32px); font-size: 18px; }
  .subtitle { font-size: clamp(10px, 1.4cqw, 13px); }
}
</style>
