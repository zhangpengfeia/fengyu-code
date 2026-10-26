<script setup lang="ts">
defineProps<{ modelValue: number | undefined; volumes?: number[] }>();
defineEmits<{ "update:modelValue": [value: number | undefined] }>();
</script>

<template>
  <div class="volume-filter" role="group" aria-label="按册次筛选">
    <button
      :class="{ active: modelValue === undefined }"
      :aria-pressed="modelValue === undefined"
      @click="$emit('update:modelValue', undefined)"
    >
      全部册次
    </button>
    <button
      v-for="volume in volumes ?? [1, 2, 3, 4, 5, 6]"
      :key="volume"
      :class="{ active: modelValue === volume }"
      :aria-pressed="modelValue === volume"
      @click="$emit('update:modelValue', volume)"
    >
      第 {{ volume }} 册
    </button>
  </div>
</template>

<style scoped>
.volume-filter {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 3px 0 8px;
  scrollbar-width: thin;
}
button {
  flex: 0 0 auto;
  min-height: 36px;
  border: 1px solid var(--line);
  background: white;
  padding: 7px 17px;
  border-radius: 12px;
  color: #7d89a7;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s;
}
button:hover {
  border-color: #a5b8ff;
}
button.active {
  color: #fff;
  border-color: var(--primary);
  background: var(--primary);
  box-shadow: 0 4px 10px #305bff1a;
}
</style>
