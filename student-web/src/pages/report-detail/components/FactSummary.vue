<script setup lang="ts">
import { computed } from "vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import { formatDuration } from "@/features/learning/api";
import type { EvaluationRecord } from "@/features/learning/types";

const props = defineProps<{ report: EvaluationRecord }>();
const stats = computed(() => [
  {
    icon: "clock",
    label: "本局用时",
    value: formatDuration(props.report.durationMs),
    tone: "blue",
  },
  {
    icon: "target",
    label: "实际操作",
    value:
      props.report.actualSteps === undefined
        ? "—"
        : `${props.report.actualSteps} 步`,
    tone: "purple",
  },
  {
    icon: "sparkles",
    label: "最优参考",
    value:
      props.report.optimalSteps == null
        ? "未提供"
        : `${props.report.optimalSteps} 步`,
    tone: "orange",
  },
  {
    icon: "check",
    label: "通关结果",
    value:
      props.report.passed === undefined
        ? "待生成"
        : props.report.passed
          ? "已通关"
          : "继续挑战",
    tone: "green",
  },
]);
</script>

<template>
  <div class="fact-summary">
    <div v-for="stat in stats" :key="stat.label" class="fact-item">
      <span class="fact-icon" :class="stat.tone"
        ><AppIcon :name="stat.icon" :size="19"
      /></span>
      <div>
        <span>{{ stat.label }}</span
        ><strong>{{ stat.value }}</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fact-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.fact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 21px 18px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 17px;
  min-width: 0;
}
.fact-icon {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 14px;
}
.blue {
  background: #edf3ff;
  color: #7199ed;
}
.purple {
  background: #f1edff;
  color: #a088e3;
}
.orange {
  background: #fff3e2;
  color: #eeb269;
}
.green {
  background: #e6f7f1;
  color: #64bfa6;
}
.fact-item div {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.fact-item div > span {
  font-size: 10px;
  color: var(--muted);
}
.fact-item strong {
  font-size: 17px;
  color: var(--ink);
}
@media (max-width: 1050px) {
  .fact-item {
    padding: 17px 12px;
    gap: 9px;
  }
  .fact-icon {
    width: 34px;
    height: 34px;
    border-radius: 11px;
  }
  .fact-item strong {
    font-size: 14px;
  }
}
@media (max-width: 680px) {
  .fact-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
  .fact-item {
    padding: 18px 16px;
  }
  .fact-item strong {
    font-size: 16px;
  }
}
</style>
