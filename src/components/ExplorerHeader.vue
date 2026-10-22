<script setup lang="ts">
import { artwork } from '../assets'

defineProps<{ page: number }>()
</script>

<template>
  <header class="profile">
    <img :src="artwork.profile" alt="杨同学，大师" />
  </header>
  <div class="top-actions">
    <span class="chapter">星球目录 · {{ page === 0 ? '01 — 06' : '07 — 12' }}</span>
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

  img {
    width: 100%;
    display: block;
  }
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
  letter-spacing: 0.12em;
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
  letter-spacing: 0.22em;
  font-size: 1.1cqw;
  color: #d3e4ff;
  z-index: 38;
  text-shadow: 0 2px 6px #14254d;
}

@media @compact-scene {
  .top-actions {
    top: 4%;
    gap: 8px;
  }
  .chapter {
    display: none;
  }
  .subtitle {
    font-size: clamp(10px, 1.4cqw, 13px);
  }
}
</style>
