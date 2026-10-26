<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number | string }>(), {
  size: 20,
});
const paths: Record<string, string> = {
  grid: "M3 3h7v7H3Zm11 0h7v7h-7ZM3 14h7v7H3Zm11 0h7v7h-7Z",
  shapes: "m7 3 5 8H2ZM15 4h6v6h-6Zm5 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
  palette:
    "M21 12a9 9 0 1 0-9 9h1a2 2 0 0 0 2-2c0-1-1-2-1-3s1-2 2-2h3a2 2 0 0 0 2-2ZM7 8h.01M11 6h.01M16 8h.01M6 13h.01",
  layers: "m12 2 10 6-10 6L2 8Zm-10 10 10 6 10-6M2 16l10 6 10-6",
  info: "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-1v6m0-10h.01",
  home: "m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z",
  book: "M12 5C8 2 3 4 3 4v15s5-2 9 1m0-15c4-3 9-1 9-1v15s-5-2-9 1Zm0 0v15",
  courses: "M12 5C8 2 3 4 3 4v15s5-2 9 1m0-15c4-3 9-1 9-1v15s-5-2-9 1Zm0 0v15",
  report: "M5 3h10l4 4v14H5Zm9 0v5h5M8 16v2m4-6v6m4-8v8",
  chart: "M4 3v17h17M8 16v-4m5 4V7m5 9V4",
  trophy:
    "M8 3h8v6a4 4 0 0 1-8 0ZM8 5H4v2a4 4 0 0 0 4 4m8-6h4v2a4 4 0 0 1-4 4m-4 2v5m-4 3h8m-10 0h12",
  user: "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2",
  chat: "M21 11a9 9 0 0 1-9 9 10 10 0 0 1-4-1l-5 2 2-5a10 10 0 0 1-2-5 9 9 0 0 1 18 0ZM8 11h.01M12 11h.01M16 11h.01",
  sparkles:
    "m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5ZM20 2v4m-2-2h4",
  star: "m12 3 2.8 5.7L21 9.6l-4.5 4.5 1 6.3-5.5-3-5.5 3 1-6.3L3 9.6l6.2-.9Z",
  settings:
    "m9 3-.5 3-2.5 1-2.5-1-2 3 2 2v2l-2 2 2 3 2.5-1 2.5 1 .5 3h4l.5-3 2.5-1 2.5 1 2-3-2-2v-2l2-2-2-3-2.5 1-2.5-1-.5-3Zm6 9a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
  search: "M20 20l-5-5m2-6a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z",
  "arrow-right": "M4 12h16m-6-6 6 6-6 6",
  "arrow-left": "M20 12H4m6-6-6 6 6 6",
  "chevron-right": "m9 5 7 7-7 7",
  "chevron-down": "m5 9 7 7 7-7",
  check: "m5 12 4 4L19 6",
  close: "m6 6 12 12M6 18 18 6",
  plus: "M12 4v16M4 12h16",
  send: "m22 2-7 20-4-9L2 9Zm0 0L11 13",
  stop: "M6 6h12v12H6Z",
  play: "m8 4 12 8-12 8Z",
  lock: "M5 10h14v11H5Zm3 0V6a4 4 0 0 1 8 0v4m-4 4v3",
  eye: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
  logout: "M9 3H4v18h5m5-5 5-4-5-4m-6 4h13",
  volume: "m11 4-6 5H2v6h3l6 5Zm4 4a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14",
  upload: "M12 16V3m-5 5 5-5 5 5M4 15v6h16v-6",
  refresh: "M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5m-4 8a8 8 0 0 0 14 3l3-3m0 5v-5h-5",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-9 12a3 3 0 0 0 6 0",
  calendar: "M4 5h16v16H4Zm3-3v6m10-6v6M4 10h16m-12 4h1m6 0h1m-8 3h1",
  clock: "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-5v6l4 2",
  medal:
    "m7 3 5 7 5-7m-4 7L9 3m3 6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Zm0 3 1 2 2 .3-1.5 1.5.4 2.2-1.9-1-1.9 1 .4-2.2L9 14.3l2-.3Z",
  help: "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-12-3a3 3 0 1 1 5 2c-1 1-2 1-2 3m0 3h.01",
  shield: "m12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6Zm-4 10 3 3 5-6",
  target:
    "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-4 0a5 5 0 1 1-10 0 5 5 0 0 1 10 0Zm-5 0h.01",
};
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="paths[name] || paths.sparkles" />
  </svg>
</template>
