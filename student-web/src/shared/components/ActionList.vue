<script setup lang="ts">
defineProps<{ label: string }>();
</script>
<template>
  <ul class="action-list" :aria-label="label">
    <slot />
  </ul>
</template>
<style scoped>
.action-list {
  list-style: none;
  margin: 0;
  padding: 0 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
  overflow: hidden;
}
.action-list :deep(.action-list-item + .action-list-item) {
  border-top: 1px solid var(--line);
}
@media (max-width: 767px) {
  .action-list {
    padding: 0 16px;
    border-radius: 16px;
  }
}
</style>
