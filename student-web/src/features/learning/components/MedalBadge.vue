<script setup lang="ts">
import type { Medal } from "../types";

withDefaults(
  defineProps<{ medal?: Medal; size?: number; showLabel?: boolean }>(),
  {
    medal: "NONE",
    size: 44,
    showLabel: false,
  },
);
const labels: Record<Medal, string> = {
  GOLD: "金星",
  SILVER: "银星",
  BRONZE: "铜星",
  NONE: "继续加油",
};
</script>

<template>
  <span class="medal-wrap" :class="medal.toLowerCase()">
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 56 64"
      role="img"
      :aria-label="labels[medal]"
    >
      <path d="m14 33 2 28 12-7 12 7 2-28" fill="var(--ribbon)" />
      <circle cx="28" cy="25" r="23" fill="var(--medal-edge)" />
      <circle
        cx="28"
        cy="25"
        r="18.5"
        fill="var(--medal-fill)"
        stroke="var(--medal-light)"
        stroke-width="2"
      />
      <path
        d="m28 11 4.1 8.4 9.3 1.3-6.7 6.6 1.6 9.2L28 32.2l-8.3 4.3 1.6-9.2-6.7-6.6 9.3-1.3Z"
        fill="var(--medal-light)"
      />
      <path
        d="m13 13 4-4M12 18l2-2"
        stroke="white"
        stroke-width="2.5"
        stroke-linecap="round"
        opacity=".8"
      />
    </svg>
    <span v-if="showLabel">{{ labels[medal] }}</span>
  </span>
</template>

<style scoped>
.medal-wrap {
  --ribbon: #b6c2d5;
  --medal-edge: #c6d1e4;
  --medal-fill: #e3e9f3;
  --medal-light: #fff;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--muted);
}
.gold {
  --ribbon: #ff9758;
  --medal-edge: #ffbd40;
  --medal-fill: #ffdc72;
  --medal-light: #fff7d3;
}
.silver {
  --ribbon: #8daef0;
  --medal-edge: #a8bfdd;
  --medal-fill: #d4e4f1;
  --medal-light: #f7fcff;
}
.bronze {
  --ribbon: #e89575;
  --medal-edge: #d5956d;
  --medal-fill: #ecc19a;
  --medal-light: #fff0da;
}
svg {
  display: block;
  overflow: visible;
}
</style>
