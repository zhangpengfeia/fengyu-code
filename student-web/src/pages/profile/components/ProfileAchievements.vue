<script setup lang="ts">
import { isPreview } from "@/shared/config";
import AppIcon from "@/shared/components/AppIcon.vue";
import AchievementBadge from "./AchievementBadge.vue";
const achievements = [
  { name: "初次探索", color: "#ff9d42", icon: "star" },
  { name: "思维新星", color: "#5676ff", icon: "sparkles" },
  { name: "坚持不懈", color: "#9162ef", icon: "medal" },
  { name: "学习达人", color: "#ef729d", icon: "trophy" },
  { name: "奇思妙想", color: "#31bfaa", icon: "target" },
  { name: "勇于挑战", color: "#f3b745", icon: "shield" },
];
</script>
<template>
  <template v-if="isPreview"
    ><span class="badge">示例徽章</span>
    <div class="achievements">
      <AchievementBadge
        v-for="item in achievements"
        :key="item.name"
        v-bind="item"
      /></div
  ></template>
  <div v-else class="honor-empty">
    <AppIcon name="medal" :size="36" />
    <p>完成测评后，可在学习报告中查看本局徽章。</p>
    <RouterLink class="btn btn-soft" to="/reports">查看测评报告</RouterLink>
  </div>
</template>
<style scoped>
.achievements {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  justify-items: center;
  gap: 26px 18px;
  padding: 26px 0 10px;
}
.honor-empty {
  text-align: center;
  color: var(--muted);
  padding: 16px 0;
}
.honor-empty p {
  font-size: 13px;
  line-height: 1.8;
  margin-top: 16px;
}
</style>
