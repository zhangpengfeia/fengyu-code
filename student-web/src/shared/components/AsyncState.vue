<script setup lang="ts">
import AppIcon from "./AppIcon.vue";
withDefaults(
  defineProps<{
    loading?: boolean;
    error?: string;
    empty?: boolean;
    emptyTitle?: string;
  }>(),
  { loading: false, error: "", empty: false, emptyTitle: "暂时没有内容" },
);
defineEmits<{ retry: [] }>();
</script>
<template>
  <div v-if="loading" class="async-state" role="status">
    <span class="spinner" />
    <p>正在为你准备…</p>
  </div>
  <div v-else-if="error" class="async-state" role="alert">
    <AppIcon name="help" :size="36" />
    <h3>暂时无法加载</h3>
    <p>{{ error }}</p>
    <button class="btn btn-soft" @click="$emit('retry')">
      <AppIcon name="refresh" :size="16" />重新加载
    </button>
  </div>
  <div v-else-if="empty" class="async-state">
    <AppIcon name="book" :size="36" />
    <h3>{{ emptyTitle }}</h3>
    <p>新的探索正在路上。</p>
  </div>
  <slot v-else />
</template>
<style scoped>
.async-state {
  min-height: 210px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--muted);
  padding: 28px;
}
.async-state h3 {
  color: var(--ink);
  font-size: 16px;
  margin: 0;
}
.async-state p {
  max-width: 440px;
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
}
</style>
