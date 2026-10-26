<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import { isPreview } from "@/shared/config";
import { listEvaluations } from "@/features/learning/api";
import { previewAbilities } from "@/features/learning/preview";
import type { EvaluationRecord } from "@/features/learning/types";
import AbilityRadar from "@/features/learning/components/AbilityRadar.vue";
import VolumeFilter from "@/features/learning/components/VolumeFilter.vue";
import ReportCard from "./components/ReportCard.vue";

const records = ref<EvaluationRecord[]>([]);
const loading = ref(true);
const loadingMore = ref(false);
const error = ref("");
const moreError = ref("");
const hasMore = ref(false);
const volume = ref<number>();
const pageSize = 20;
let requestVersion = 0;
let nextOffset = 0;
const visibleRecords = computed(() =>
  records.value.filter(
    (item) => volume.value === undefined || item.volume === volume.value,
  ),
);
const volumes = computed(() =>
  [
    ...new Set(
      records.value
        .map((item) => item.volume)
        .filter((item): item is number => item !== undefined),
    ),
  ].sort((a, b) => a - b),
);
const completed = computed(
  () =>
    records.value.filter((item) => item.status.startsWith("COMPLETED")).length,
);
const gold = computed(
  () => records.value.filter((item) => item.medal === "GOLD").length,
);
async function load() {
  const version = ++requestVersion;
  loading.value = true;
  loadingMore.value = false;
  error.value = "";
  moreError.value = "";
  try {
    const result = await listEvaluations(pageSize);
    if (version !== requestVersion) return;
    records.value = result;
    nextOffset = result.length;
    hasMore.value = result.length === pageSize;
  } catch (cause) {
    if (version === requestVersion) {
      error.value =
        cause instanceof Error ? cause.message : "报告加载失败，请稍后重试。";
    }
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}
async function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return;
  const version = requestVersion;
  loadingMore.value = true;
  moreError.value = "";
  try {
    const next = await listEvaluations(pageSize, nextOffset);
    if (version !== requestVersion) return;
    nextOffset += next.length;
    // New reports may shift offset pagination between requests. Keep identities unique.
    const existingIds = new Set(records.value.map((item) => item.id));
    records.value.push(...next.filter((item) => !existingIds.has(item.id)));
    hasMore.value = next.length === pageSize;
  } catch (cause) {
    if (version === requestVersion) {
      moreError.value =
        cause instanceof Error ? cause.message : "更多报告加载失败。";
    }
  } finally {
    if (version === requestVersion) loadingMore.value = false;
  }
}
onMounted(load);
onBeforeUnmount(() => {
  requestVersion += 1;
});
</script>

<template>
  <div class="page-stack reports-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的学习旅程</p>
        <h1>每一步成长，都值得记录<span class="heading-spark">✦</span></h1>
        <p class="muted">发现自己的闪光点，和昨天的自己一起进步。</p>
      </div>
      <button
        class="btn btn-soft refresh-btn"
        :disabled="loading || loadingMore"
        @click="load"
      >
        <AppIcon name="refresh" :size="16" />刷新报告
      </button>
    </div>
    <div class="report-overview">
      <section class="growth-card card">
        <div class="section-heading">
          <div>
            <span class="little-label">成长画像</span>
            <h2>我的能力探索</h2>
          </div>
          <span v-if="isPreview" class="preview-tag">示例数据</span>
        </div>
        <template v-if="isPreview"
          ><AbilityRadar :values="previewAbilities" />
          <p class="chart-note">每一种能力，都有无限可能</p></template
        >
        <div v-else class="ability-empty">
          <div class="ability-symbol"><AppIcon name="chart" :size="32" /></div>
          <h3>期待下一次能力发现</h3>
          <p>综合能力画像暂未开放，你可以先查看下方的单局测评报告。</p>
          <RouterLink to="/courses" class="btn btn-soft"
            >去探索课程<AppIcon name="arrow-right" :size="14"
          /></RouterLink>
        </div>
      </section>
      <div class="overview-side">
        <section class="summary-card card">
          <div class="summary-text">
            <span class="little-label">{{
              isPreview ? "示例成长记录" : "已加载记录统计"
            }}</span>
            <h2>小小积累，大大进步</h2>
            <p class="muted">每一次认真尝试，都是成长的印记。</p>
          </div>
          <div class="summary-numbers">
            <div>
              <span class="number-icon purple"
                ><AppIcon name="report" :size="19" /></span
              ><strong>{{ loading || error ? "—" : completed }}</strong
              ><span>已完成报告</span>
            </div>
            <div>
              <span class="number-icon gold"
                ><AppIcon name="star" :size="19" /></span
              ><strong>{{ loading || error ? "—" : gold }}</strong
              ><span>获得金星</span>
            </div>
            <div>
              <span class="number-icon blue"
                ><AppIcon name="book" :size="19" /></span
              ><strong>{{ loading || error ? "—" : volumes.length }}</strong
              ><span>探索册次</span>
            </div>
          </div>
        </section>
        <section class="encourage-card">
          <div class="encourage-icon">
            <AppIcon name="sparkles" :size="24" />
          </div>
          <div>
            <h3>成长有自己的节奏</h3>
            <p>分数只是一个小小的标记。试着回顾解题过程，发现新的思考方法。</p>
            <RouterLink to="/companion"
              >和智能学伴聊一聊<AppIcon name="arrow-right" :size="14"
            /></RouterLink>
          </div>
          <span class="encourage-spark">✧</span>
        </section>
      </div>
    </div>
    <section class="records-section">
      <div class="section-heading">
        <div>
          <span class="little-label">每一次探索的足迹</span>
          <h2>
            我的测评报告<span class="count-chip">{{ records.length }}</span>
          </h2>
        </div>
        <span class="muted record-hint">单局测评 · 智能分析</span>
      </div>
      <VolumeFilter v-model="volume" :volumes="volumes" />
      <AsyncState
        :loading="loading"
        :error="error"
        :empty="visibleRecords.length === 0"
        :empty-title="volume ? '这一册暂时没有报告' : '还没有测评报告'"
        @retry="load"
      >
        <div class="report-grid">
          <ReportCard
            v-for="record in visibleRecords"
            :key="record.id"
            :report="record"
          />
        </div>
      </AsyncState>
      <div v-if="!loading && !error && hasMore" class="load-more">
        <p v-if="moreError" class="load-error" role="alert">{{ moreError }}</p>
        <button class="btn btn-soft" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? "正在加载…" : "加载更多报告" }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.heading-spark {
  color: #ffb854;
  margin-left: 9px;
  font-size: 24px;
}
.refresh-btn {
  align-self: flex-end;
}
.report-overview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 22px;
}
.growth-card {
  padding: 25px 26px 16px;
  min-width: 0;
}
.little-label {
  font-size: 10px;
  color: var(--muted);
}
.section-heading h2,
.summary-text h2 {
  margin: 7px 0 0;
  font-size: 19px;
}
.preview-tag {
  color: #8675bd;
  background: #f1edfc;
  padding: 5px 9px;
  border-radius: 7px;
  font-size: 10px;
}
.chart-note {
  text-align: center;
  color: #a0aac0;
  font-size: 11px;
  margin: -3px 0 4px;
}
.overview-side {
  display: flex;
  flex-direction: column;
  gap: 19px;
}
.summary-card {
  padding: 25px 27px;
  flex: 1;
}
.summary-text p {
  font-size: 11px;
  margin-top: 9px;
}
.summary-numbers {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 28px;
}
.summary-numbers > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border-right: 1px solid var(--line);
}
.summary-numbers > div:last-child {
  border: 0;
}
.number-icon {
  height: 36px;
  width: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
}
.purple {
  background: #f0eaff;
  color: #a17be8;
}
.gold {
  background: #fff5dd;
  color: #eeb543;
}
.blue {
  background: #eaf1ff;
  color: #6698ed;
}
.summary-numbers strong {
  font-size: 29px;
  font-weight: 700;
  color: var(--ink);
}
.summary-numbers > div > span:last-child {
  font-size: 11px;
  color: var(--muted);
}
.encourage-card {
  display: flex;
  position: relative;
  overflow: hidden;
  gap: 14px;
  background: linear-gradient(115deg, #eef0ff, #f6edff);
  padding: 23px;
  border: 1px solid #e8e4fa;
  border-radius: 19px;
}
.encourage-icon {
  background: #fff;
  color: #a18adf;
  width: 43px;
  height: 43px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.encourage-card h3 {
  margin: 0 0 7px;
  font-size: 15px;
}
.encourage-card p {
  max-width: 310px;
  margin: 0;
  font-size: 11px;
  line-height: 1.9;
  color: #8d89a7;
}
.encourage-card a {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
  color: #7d70ca;
  font-size: 11px;
  font-weight: 600;
  text-decoration: none;
}
.encourage-spark {
  position: absolute;
  right: 18px;
  top: 12px;
  color: #d4c8fa;
  font-size: 71px;
  transform: rotate(10deg);
  pointer-events: none;
}
.records-section {
  margin-top: 4px;
}
.records-section > .section-heading {
  margin-bottom: 20px;
}
.count-chip {
  display: inline-block;
  font-size: 11px;
  margin-left: 9px;
  background: #e9efff;
  color: #6b88de;
  padding: 3px 8px;
  border-radius: 6px;
  vertical-align: middle;
}
.record-hint {
  font-size: 11px;
}
.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 17px;
  margin-top: 13px;
}
.load-more {
  text-align: center;
  margin: 22px 0 0;
}
.load-error {
  color: #ca6576;
  font-size: 12px;
}
.ability-empty {
  min-height: 270px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 8px;
}
.ability-symbol {
  width: 65px;
  height: 65px;
  border-radius: 22px;
  background: #f0f4ff;
  color: #8ca7ed;
  display: grid;
  place-items: center;
}
.ability-empty h3 {
  font-size: 15px;
  margin: 9px 0 0;
}
.ability-empty p {
  max-width: 280px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--muted);
}
.ability-empty a {
  font-size: 12px;
}
@media (max-width: 1050px) {
  .report-overview {
    gap: 16px;
    grid-template-columns: 1fr 1fr;
  }
  .summary-card {
    padding: 22px 18px;
  }
  .growth-card {
    padding: 22px 18px 15px;
  }
  .report-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 650px) {
  .report-overview {
    grid-template-columns: 1fr;
  }
  .growth-card {
    padding: 20px;
  }
  .overview-side {
    gap: 15px;
  }
  .summary-numbers {
    margin-top: 22px;
  }
  .summary-card {
    padding: 22px;
  }
  .refresh-btn {
    font-size: 11px;
    min-height: 34px;
    padding: 7px 10px;
  }
  .heading-spark,
  .record-hint {
    display: none;
  }
  .encourage-card {
    padding: 21px;
  }
  .report-grid {
    margin-top: 10px;
  }
  .records-section > .section-heading {
    margin-bottom: 16px;
  }
}
</style>
