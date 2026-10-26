<script setup lang="ts">
withDefaults(defineProps<{ size?: number | string }>(), { size: 48 });
</script>
<template>
  <span
    class="mascot-avatar"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  >
    <svg viewBox="0 0 100 100" fill="none">
      <circle cx="50" cy="50" r="47" fill="#d9f4ff" />
      <path d="M50 23v-8" stroke="#315bff" stroke-width="4" />
      <circle cx="50" cy="13" r="5" fill="#ffb85b" />
      <rect x="20" y="29" width="60" height="49" rx="20" fill="#26bfc8" />
      <path d="M25 52c0-21 50-21 50 0v9c0 18-50 18-50 0Z" fill="white" />
      <path
        d="M35 54v6m30-6v6"
        stroke="#28479b"
        stroke-width="5"
        stroke-linecap="round"
      />
      <path
        d="M43 65q7 7 14 0"
        stroke="#28479b"
        stroke-width="3"
        stroke-linecap="round"
      />
      <circle cx="30" cy="63" r="4" fill="#ffbbb7" />
      <circle cx="70" cy="63" r="4" fill="#ffbbb7" />
      <path
        d="M18 48v13m64-13v13"
        stroke="#315bff"
        stroke-width="7"
        stroke-linecap="round"
      />
      <path
        d="M36 82h28"
        stroke="#26bfc8"
        stroke-width="7"
        stroke-linecap="round"
      />
    </svg>
  </span>
</template>
<style scoped>
.mascot-avatar {
  display: inline-flex;
  flex-shrink: 0;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 3px 12px #315bff15;
  overflow: hidden;
}
.mascot-avatar svg {
  width: 100%;
  height: 100%;
}
</style>
