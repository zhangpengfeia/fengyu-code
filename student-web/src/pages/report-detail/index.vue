<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import { getEvaluation, formatReportDate } from "@/features/learning/api";
import type { EvaluationRecord } from "@/features/learning/types";
import MedalBadge from "@/features/learning/components/MedalBadge.vue";
import FactSummary from "./components/FactSummary.vue";
import { isPreview } from "@/shared/config";

const route = useRoute();
const report = ref<EvaluationRecord>();
const loading = ref(true);
const error = ref("");
let requestVersion = 0;
async function load() {
  const version = ++requestVersion;
  loading.value = true;
  error.value = "";
  try {
    const result = await getEvaluation(String(route.params.id));
    if (version === requestVersion) report.value = result;
  } catch (cause) {
    if (version === requestVersion)
      error.value =
        cause instanceof Error ? cause.message : "报告加载失败，请稍后重试。";
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}
watch(() => route.params.id, load, { immediate: true });
onBeforeUnmount(() => {
  requestVersion += 1;
});
</script>

<template>
  <div class="page-stack detail-page">
    <RouterLink to="/reports" class="back-link"
      ><AppIcon name="arrow-left" :size="16" />返回学习报告</RouterLink
    >
    <AsyncState :loading="loading" :error="error" @retry="load">
      <template v-if="report">
        <header class="report-hero">
          <div>
            <p class="eyebrow">
              学习测评报告
              <span v-if="isPreview" class="preview-tag">示例报告</span>
            </p>
            <h1>{{ report.title }}</h1>
            <p class="hero-meta">
              {{ report.volume ? `第 ${report.volume} 册` : "单局测评"
              }}<span>·</span>{{ formatReportDate(report.updatedAt)
              }}<span>·</span>游戏测评报告
            </p>
            <p class="hero-note">每一次思考，都让你离更好的自己近一点。</p>
          </div>
          <MedalBadge :medal="report.medal" :size="112" show-label /><span
            class="hero-spark one"
            >✧</span
          ><span class="hero-spark two">✦</span>
        </header>
        <FactSummary :report="report" />
        <div
          v-if="!report.status.startsWith('COMPLETED')"
          class="pending-panel card"
          role="status"
        >
          <AppIcon name="clock" :size="25" />
          <div>
            <h3>
              {{
                report.status === "FAILED"
                  ? "报告暂未生成成功"
                  : report.status === "SKIPPED_TRAINING"
                    ? "训练模式不生成测评报告"
                    : "智能导师正在整理报告"
              }}
            </h3>
            <p>
              {{
                report.status === "FAILED"
                  ? "请稍后刷新查看，或联系老师确认测评数据与服务状态。"
                  : report.status === "SKIPPED_TRAINING"
                    ? "训练中的每一次尝试也很有意义，完成测评后可以查看单局分析。"
                    : "服务正在处理测评数据，你可以稍后刷新查看。"
              }}
            </p>
          </div>
          <button class="btn btn-soft" @click="load">刷新</button>
        </div>
        <div class="analysis-layout">
          <section class="analysis-card card">
            <div class="analysis-heading">
              <MascotAvatar :size="43" />
              <div>
                <span class="little-label">智能学伴</span>
                <h2>智能导师说</h2>
              </div>
              <span class="analysis-label">{{
                report.generationMode === "TEMPLATE"
                  ? "基础模板"
                  : report.generationMode === "PREVIEW"
                    ? "示例分析"
                    : "本局分析"
              }}</span>
            </div>
            <p v-if="report.performance" class="performance">
              {{ report.performance }}
            </p>
            <p v-else class="performance muted">
              本局分析尚未生成，完成后将展示在这里。
            </p>
            <div class="analysis-tip">
              <AppIcon name="sparkles" :size="18" />
              <p>关注思考的方法，也别忘了为自己的坚持鼓掌。</p>
            </div>
            <RouterLink
              :to="{
                path: '/companion',
                query: report.gameCode ? { game: report.gameCode } : undefined,
              }"
              class="btn btn-primary"
              >和学伴聊聊这局游戏<AppIcon name="arrow-right" :size="16"
            /></RouterLink>
          </section>
          <section class="suggestions-card card">
            <div class="section-heading">
              <div>
                <span class="little-label">下一步，可以这样做</span>
                <h2>给你的成长建议</h2>
              </div>
              <AppIcon name="target" :size="23" />
            </div>
            <ol v-if="report.suggestions.length" class="suggestion-list">
              <li
                v-for="(suggestion, index) in report.suggestions"
                :key="suggestion"
              >
                <span>{{ String(index + 1).padStart(2, "0") }}</span>
                <p>{{ suggestion }}</p>
              </li>
            </ol>
            <p v-else class="muted no-suggestions">
              本局暂时没有建议，稍后再来看看吧。
            </p>
            <RouterLink to="/courses" class="practice-link"
              >再去探索一个游戏<AppIcon name="arrow-right" :size="15"
            /></RouterLink>
          </section>
        </div>
        <section v-if="report.limitations.length" class="limitations">
          <AppIcon name="help" :size="16" />
          <div>
            <strong>报告说明</strong>
            <p v-for="limitation in report.limitations" :key="limitation">
              {{ limitation }}
            </p>
          </div>
        </section>
        <p class="report-footnote">
          本报告基于本局实际测评记录生成，仅反映当前一次游戏表现。
        </p>
      </template>
    </AsyncState>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 7px;
  text-decoration: none;
  color: #8994ad;
  font-size: 12px;
}
.back-link:hover {
  color: var(--primary);
}
.report-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 25px;
  overflow: hidden;
  min-height: 216px;
  padding: 30px 45px;
  background: linear-gradient(115deg, #e6edff, #ebe6ff 65%, #fff0e9);
  border: 1px solid #e0e5fa;
  border-radius: 24px;
}
.report-hero > div,
.report-hero > .medal-wrap {
  z-index: 1;
}
.report-hero h1 {
  font-size: 31px;
  margin: 12px 0 15px;
  letter-spacing: -0.5px;
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 11px;
  font-size: 11px;
  color: #8890b4;
}
.hero-meta span {
  color: #b0b6ce;
}
.hero-note {
  margin-top: 19px;
  font-size: 12px;
  color: #727da6;
}
.hero-spark {
  position: absolute;
  color: #fff;
  pointer-events: none;
}
.hero-spark.one {
  right: 165px;
  top: 8px;
  font-size: 99px;
  transform: rotate(15deg);
}
.hero-spark.two {
  right: 25px;
  bottom: 8px;
  font-size: 53px;
}
.preview-tag {
  font-size: 9px;
  letter-spacing: 0;
  background: #ffffff8c;
  border-radius: 5px;
  padding: 4px 7px;
  margin-left: 10px;
}
.analysis-layout {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 22px;
}
.analysis-card,
.suggestions-card {
  padding: 28px;
}
.analysis-heading {
  display: flex;
  align-items: center;
  gap: 11px;
}
.little-label {
  font-size: 10px;
  color: var(--muted);
}
.analysis-heading h2,
.section-heading h2 {
  margin: 5px 0 0;
  font-size: 18px;
}
.analysis-label {
  font-size: 9px;
  background: #edf2ff;
  color: #7b92ca;
  border-radius: 6px;
  padding: 5px 7px;
  margin-left: auto;
}
.performance {
  line-height: 2.1;
  font-size: 13px;
  color: #75809f;
  white-space: pre-line;
  margin: 23px 0;
}
.analysis-tip {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 15px;
  background: #f7f8ff;
  border-radius: 11px;
  color: #a39ad0;
  margin-bottom: 23px;
}
.analysis-tip svg {
  flex-shrink: 0;
  margin-top: 1px;
}
.analysis-tip p {
  margin: 0;
  color: #9993b4;
  font-size: 11px;
  line-height: 1.8;
}
.analysis-card > .btn {
  font-size: 12px;
}
.suggestions-card > .section-heading > svg {
  color: #9c8ee2;
}
.suggestion-list {
  list-style: none;
  margin: 24px 0 19px;
  padding: 0;
}
.suggestion-list li {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  margin: 0 0 21px;
}
.suggestion-list li > span {
  flex-shrink: 0;
  height: 29px;
  width: 29px;
  border-radius: 9px;
  background: #f0edff;
  color: #a18ad7;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
}
.suggestion-list li:nth-child(2) > span {
  background: #fff2e8;
  color: #e4ac7b;
}
.suggestion-list li:nth-child(3) > span {
  background: #e9f5f3;
  color: #80b9aa;
}
.suggestion-list p {
  font-size: 12px;
  line-height: 1.9;
  color: #7b87a2;
  margin: 2px 0 0;
}
.practice-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}
.no-suggestions {
  padding: 30px 0;
  font-size: 13px;
}
.limitations {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px 19px;
  border-radius: 12px;
  border: 1px solid #e5e8f2;
  background: #f8f9fd;
  color: #9aa4bc;
}
.limitations > svg {
  flex-shrink: 0;
  margin-top: 2px;
}
.limitations strong {
  font-size: 11px;
  font-weight: 600;
}
.limitations p {
  font-size: 11px;
  line-height: 1.8;
  margin: 5px 0 0;
}
.report-footnote {
  text-align: center;
  font-size: 10px;
  color: #acb4c6;
  margin: 0;
}
.pending-panel {
  display: flex;
  align-items: flex-start;
  gap: 13px;
  padding: 22px;
  color: #9d95bd;
}
.pending-panel > svg {
  flex-shrink: 0;
}
.pending-panel h3 {
  margin: 0;
  font-size: 14px;
  color: var(--ink);
}
.pending-panel p {
  font-size: 12px;
  line-height: 1.8;
  margin: 7px 0 0;
}
.pending-panel .btn {
  margin-left: auto;
  flex-shrink: 0;
}
@media (max-width: 800px) {
  .analysis-layout {
    grid-template-columns: 1fr;
    gap: 17px;
  }
  .report-hero {
    padding: 27px;
  }
  .report-hero h1 {
    font-size: 27px;
  }
  .analysis-card,
  .suggestions-card {
    padding: 24px;
  }
}
@media (max-width: 520px) {
  .report-hero {
    padding: 23px;
    min-height: 195px;
    gap: 13px;
    border-radius: 19px;
  }
  .report-hero h1 {
    font-size: 25px;
  }
  .report-hero > .medal-wrap :deep(svg) {
    width: 73px;
    height: 83px;
  }
  .hero-note {
    font-size: 11px;
    line-height: 1.8;
    max-width: 180px;
  }
  .hero-meta {
    gap: 7px;
    font-size: 10px;
  }
  .analysis-card,
  .suggestions-card {
    padding: 21px;
  }
  .pending-panel {
    flex-wrap: wrap;
  }
  .pending-panel > .btn {
    margin-left: 38px;
  }
  .pending-panel > div {
    flex: 1;
  }
  .performance {
    font-size: 12px;
  }
}
</style>
