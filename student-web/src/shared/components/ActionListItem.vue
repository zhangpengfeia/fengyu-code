<script setup lang="ts">
import { RouterLink } from "vue-router";
import AppIcon from "./AppIcon.vue";
withDefaults(
  defineProps<{
    label: string;
    icon: string;
    to?: string;
    value?: string;
    tone?: string;
    danger?: boolean;
    disabled?: boolean;
  }>(),
  { tone: "blue" },
);
defineEmits<{ click: [] }>();
</script>
<template>
  <li class="action-list-item">
    <component
      :is="to ? RouterLink : 'button'"
      :to="to"
      :type="to ? undefined : 'button'"
      :disabled="disabled"
      :class="['list-action', { danger }]"
      @click="$emit('click')"
    >
      <span :class="['action-icon', tone]"
        ><AppIcon :name="icon" :size="20"
      /></span>
      <span class="action-label">{{ label }}</span>
      <span v-if="value" class="action-value">{{ value }}</span>
      <AppIcon name="chevron-right" :size="17" class="action-chevron" />
    </component>
  </li>
</template>
<style scoped>
.list-action {
  display: flex;
  align-items: center;
  gap: 13px;
  width: 100%;
  min-height: 70px;
  padding: 14px 0;
  border: 0;
  background: transparent;
  text-align: left;
  color: var(--ink);
}
.list-action:hover {
  background: #f8faff;
}
.action-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 11px;
  background: #eef2ff;
  color: #5c79e9;
}
.action-icon.orange {
  background: #fff5e7;
  color: #d79843;
}
.action-icon.purple {
  background: #f3eeff;
  color: #946cdb;
}
.action-icon.green {
  background: #eaf8f4;
  color: #35a78e;
}
.action-label {
  font-size: 14px;
  font-weight: 500;
}
.action-value {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  overflow-wrap: anywhere;
  text-align: right;
}
.action-chevron {
  margin-left: auto;
  flex-shrink: 0;
  color: #b0bad0;
}
.action-value + .action-chevron {
  margin-left: 0;
}
.danger .action-label {
  color: #cf5568;
}
.danger .action-icon {
  background: #fff0f2;
  color: #cf5568;
}
@media (max-width: 767px) {
  .list-action {
    min-height: 66px;
    gap: 11px;
  }
  .action-label {
    font-size: 13px;
  }
  .action-value {
    font-size: 11px;
  }
}
</style>
