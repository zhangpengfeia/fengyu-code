<script setup lang="ts">
import { Popup } from "vant";
import AppIcon from "./AppIcon.vue";
import "vant/es/popup/style";
withDefaults(
  defineProps<{ show: boolean; title: string; dismissible?: boolean }>(),
  { dismissible: true },
);
defineEmits<{ "update:show": [value: boolean] }>();
</script>
<template>
  <Popup
    :show="show"
    round
    position="bottom"
    role="dialog"
    aria-modal="true"
    :close-on-click-overlay="dismissible"
    :aria-label="title"
    :style="{
      width: '100%',
      maxWidth: '560px',
      maxHeight: '85dvh',
      left: '50%',
      transform: 'translateX(-50%)',
      padding: '28px 24px calc(26px + env(safe-area-inset-bottom))',
      overflowY: 'auto',
    }"
    @update:show="$emit('update:show', $event)"
  >
    <button
      v-if="dismissible"
      type="button"
      class="dialog-close icon-button"
      aria-label="关闭"
      @click="$emit('update:show', false)"
    >
      <AppIcon name="close" :size="18" />
    </button>
    <h2 class="dialog-title">{{ title }}</h2>
    <slot />
  </Popup>
</template>
<style scoped>
.dialog-title {
  font-size: 19px;
  margin: 0 30px 24px 0;
}
.dialog-close {
  position: absolute;
  top: 19px;
  right: 18px;
  width: 32px;
  height: 32px;
  border: 0;
  color: var(--muted);
  background: transparent;
}
</style>
