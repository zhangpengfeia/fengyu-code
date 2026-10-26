<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
defineProps<{ name: string; color: string; icon?: string }>();
</script>
<template>
  <div class="achievement">
    <span :style="{ '--badge-color': color }"
      ><AppIcon :name="icon || 'star'" :size="25" /></span
    ><strong>{{ name }}</strong>
  </div>
</template>
<style scoped>
.achievement {
  text-align: center;
}
.achievement > span {
  width: 55px;
  height: 60px;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--badge-color) 12%, white);
  color: var(--badge-color);
  clip-path: polygon(50% 0, 94% 23%, 94% 76%, 50% 100%, 6% 76%, 6% 23%);
  position: relative;
}
.achievement > span::before {
  content: "";
  position: absolute;
  inset: 5px;
  border: 1px solid var(--badge-color);
  clip-path: inherit;
  border-radius: 12px;
  opacity: 0.4;
}
.achievement strong {
  display: block;
  font-weight: 500;
  font-size: 10px;
  color: var(--muted);
  margin-top: 10px;
}
</style>
