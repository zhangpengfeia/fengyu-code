<script setup lang="ts">
import { RouterLink } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import MedalBadge from "@/features/learning/components/MedalBadge.vue";
import { formatDuration, formatReportDate } from "@/features/learning/api";
import type { EvaluationRecord } from "@/features/learning/types";

defineProps<{ report: EvaluationRecord }>();
const statusLabel = (status: string) =>
  ({
    COMPLETED: "测评完成",
    COMPLETED_WITH_FALLBACK: "基础报告",
    FAILED: "生成失败",
    SKIPPED_TRAINING: "训练记录",
    WAITING_DATA: "等待数据",
    RETRY_WAIT: "等待重试",
  })[status] ?? "正在生成";
</script>

<template>
  <RouterLink :to="`/reports/${report.id}`" class="report-card card">
    <div class="report-cover" :class="`cover-${(report.volume ?? 1) % 4}`">
      <div class="cover-spark spark-one">✦</div>
      <div class="cover-spark spark-two">✧</div>
      <MedalBadge :medal="report.medal" :size="74" />
      <span class="cover-volume">{{
        report.volume ? `第 ${report.volume} 册` : "单局测评"
      }}</span>
    </div>
    <div class="report-info">
      <div class="report-top">
        <span class="report-type">思维游戏 · 测评报告</span
        ><span
          class="report-state"
          :class="{ pending: !report.status.startsWith('COMPLETED') }"
          >{{ statusLabel(report.status) }}</span
        >
      </div>
      <h3>{{ report.title }}</h3>
      <p>{{ report.performance ?? "报告生成状态和测评详情可在这里查看。" }}</p>
      <div class="report-bottom">
        <span
          ><AppIcon name="clock" :size="13" />{{
            formatReportDate(report.updatedAt)
          }}</span
        ><span v-if="report.durationMs !== undefined">{{
          formatDuration(report.durationMs)
        }}</span
        ><span class="open-report"
          >查看报告<AppIcon name="arrow-right" :size="15"
        /></span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.report-card {
  display: flex;
  padding: 17px;
  gap: 18px;
  text-decoration: none;
  color: inherit;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  min-width: 0;
}
.report-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px #1c26500a;
}
.report-cover {
  width: 108px;
  min-height: 139px;
  flex: 0 0 auto;
  display: flex;
  position: relative;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border-radius: 15px;
  background: linear-gradient(145deg, #fff0d7, #ffe1d7);
  overflow: hidden;
}
.cover-0 {
  background: linear-gradient(145deg, #d6f4ee, #c7e9ff);
}
.cover-1 {
  background: linear-gradient(145deg, #e8e4ff, #d8e5ff);
}
.cover-2 {
  background: linear-gradient(145deg, #ffe5ee, #ffe2cf);
}
.cover-volume {
  font-size: 11px;
  font-weight: 700;
  color: #7a72a6;
}
.cover-spark {
  position: absolute;
  font-size: 21px;
  color: #fff;
}
.spark-one {
  left: 10px;
  top: 13px;
}
.spark-two {
  right: 8px;
  bottom: 38px;
  font-size: 28px;
}
.report-info {
  flex: 1;
  min-width: 0;
  padding-top: 1px;
  display: flex;
  flex-direction: column;
}
.report-top {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
  flex-wrap: wrap;
}
.report-type {
  font-size: 10px;
  color: var(--muted);
}
.report-state {
  font-size: 10px;
  color: #27a88f;
  background: #eaf9f5;
  padding: 3px 7px;
  border-radius: 5px;
}
.report-state.pending {
  color: #8a77bf;
  background: #f2edff;
}
h3 {
  font-size: 17px;
  margin: 11px 0 7px;
}
p {
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0 0 13px;
}
.report-bottom {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 13px;
  margin-top: auto;
  font-size: 10px;
  color: var(--muted);
}
.report-bottom > span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.report-bottom .open-report {
  color: var(--primary);
  font-weight: 600;
  margin-left: auto;
}
@media (max-width: 520px) {
  .report-card {
    padding: 13px;
    gap: 13px;
  }
  .report-cover {
    width: 82px;
    min-height: 129px;
  }
  .report-cover :deep(svg) {
    width: 61px;
    height: 61px;
  }
  .report-type {
    font-size: 9px;
  }
  h3 {
    font-size: 16px;
  }
  .report-bottom {
    gap: 8px;
  }
  .report-bottom .open-report {
    margin-left: 0;
  }
}
</style>
