<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import { listTutorReports } from "@/features/companion/api";
import type { TutorReport } from "@/features/companion/types";

const reports = ref<TutorReport[]>([]);
const loading = ref(false);
const error = ref("");
const selectedId = ref("");
const ready = computed(() => reports.value.filter((report) => report.report));
const selected = computed(
  () =>
    ready.value.find((report) => report.id === selectedId.value) ||
    ready.value[0],
);
const badgeNames: Record<string, string> = {
  GOLD: "金星",
  SILVER: "银星",
  BRONZE: "铜星",
  NONE: "继续加油",
};
const gameNames: Record<string, string> = {
  "eight-pieces": "八子归位",
  "arrow-maze": "箭头迷宫",
  "number-maze": "数字迷宫",
  balloon: "打气球",
  "stone-transfer": "石头搬运",
  addition: "加法游戏",
  fish: "鱼的游戏",
  "four-seas": "纵横四海",
};

async function load() {
  loading.value = true;
  error.value = "";
  try {
    reports.value = await listTutorReports();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "报告暂时无法读取。";
  } finally {
    loading.value = false;
  }
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "时间待确认"
    : new Intl.DateTimeFormat("zh-CN", {
        month: "long",
        day: "numeric",
        timeZone: "Asia/Shanghai",
      }).format(date);
}

onMounted(load);
</script>

<template>
  <div class="tutor-panel">
    <AsyncState
      :loading="loading"
      :error="error"
      :empty="!ready.length"
      empty-title="完成一次测评后，再来看看导师的建议吧"
      @retry="load"
    >
      <template v-if="selected?.report">
        <div class="tutor-grid">
          <section class="latest-report">
            <div class="report-card-title">
              <AppIcon name="report" :size="19" />
              <h3>最新测评报告</h3>
              <span>{{ formatDate(selected.updated_at) }}</span>
            </div>
            <div class="report-game">
              <span class="report-star"
                ><AppIcon name="trophy" :size="36"
              /></span>
              <div>
                <span>第 {{ selected.report.facts.volume_no }} 册</span>
                <h3>
                  {{ gameNames[selected.report.facts.game_code] || "游戏测评" }}
                </h3>
                <p>
                  {{
                    selected.report.facts.passed
                      ? "成功完成挑战，真棒！"
                      : "每一次尝试，都会离目标更近。"
                  }}
                </p>
              </div>
            </div>
            <div class="report-facts">
              <div>
                <strong>{{
                  badgeNames[selected.report.facts.badge] || "暂无徽章"
                }}</strong
                ><span>本次徽章</span>
              </div>
              <div>
                <strong
                  >{{ Math.round(selected.report.facts.duration_ms / 1000)
                  }}<small>秒</small></strong
                ><span>挑战用时</span>
              </div>
              <div>
                <strong
                  >{{ selected.report.facts.actual_steps
                  }}<small>步</small></strong
                ><span>操作次数</span>
              </div>
            </div>
            <RouterLink
              class="report-detail-link"
              :to="`/reports/${selected.id}`"
              >查看完整报告<AppIcon name="arrow-right" :size="16"
            /></RouterLink>
          </section>
          <section class="mentor-advice">
            <div class="report-card-title">
              <AppIcon name="sparkles" :size="19" />
              <h3>智能导师说</h3>
            </div>
            <p class="performance">
              {{ selected.report.evaluation.performance }}
            </p>
            <div
              v-for="(advice, index) in selected.report.evaluation.suggestions"
              :key="advice"
              class="advice-item"
            >
              <span>{{ String(index + 1).padStart(2, "0") }}</span>
              <p>{{ advice }}</p>
            </div>
            <p
              v-for="limitation in selected.report.evaluation.limitations"
              :key="limitation"
              class="report-limitation"
            >
              {{ limitation }}
            </p>
          </section>
        </div>
        <div class="report-history">
          <div class="section-heading">
            <h3>最近的测评</h3>
            <RouterLink to="/reports"
              >全部报告<AppIcon name="arrow-right" :size="14"
            /></RouterLink>
          </div>
          <button
            v-for="report in ready"
            :key="report.id"
            type="button"
            :class="{ selected: report.id === selected.id }"
            @click="selectedId = report.id"
          >
            <span class="history-icon"
              ><AppIcon name="report" :size="18" /></span
            ><span
              ><strong>{{
                gameNames[report.report!.facts.game_code] || "游戏测评"
              }}</strong
              ><small
                >第 {{ report.report!.facts.volume_no }} 册 ·
                {{ formatDate(report.updated_at) }}</small
              ></span
            ><span class="history-badge">{{
              badgeNames[report.report!.facts.badge] || "暂无徽章"
            }}</span
            ><AppIcon name="arrow-right" :size="15" />
          </button>
        </div>
      </template>
    </AsyncState>
    <div class="aggregation-note">
      <AppIcon name="sparkles" :size="17" />
      <p>综合能力与册次趋势暂未开放，当前可以查看单局测评表现和学习建议。</p>
    </div>
  </div>
</template>

<style scoped lang="less">
.tutor-panel {
  padding: 28px;
  background: #fafbff;
}
.tutor-intro {
  text-align: center;
  margin: 5px 0 28px;
  .eyebrow {
    color: #9a9dcb;
    font-size: 9px;
  }
  h2 {
    margin: 12px 0 9px;
    color: #305bff;
    font-size: 24px;
  }
  p {
    color: #919db8;
    font-size: 12px;
  }
}
.preview-badge {
  display: inline-block;
  font-size: 10px;
  color: #8582ba;
  background: #efedfc;
  padding: 5px 9px;
  border-radius: 5px;
}
.tutor-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.latest-report,
.mentor-advice {
  background: white;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 22px;
}
.report-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--primary);
  h3 {
    margin: 0;
    font-size: 14px;
  }
  > span {
    margin-left: auto;
    color: #a0abc3;
    font-size: 10px;
  }
}
.report-game {
  display: flex;
  gap: 20px;
  align-items: center;
  padding: 30px 0;
  h3 {
    margin: 7px 0;
    color: #3f548b;
    font-size: 20px;
  }
  p {
    font-size: 11px;
    color: #9aa5bc;
  }
  div > span {
    font-size: 11px;
    color: #a5aeca;
  }
}
.report-star {
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  background: linear-gradient(145deg, #fff8db, #fff0bd);
  color: #ecba3e;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 5px #fff9e9;
}
.report-facts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 22px;
  > div {
    text-align: center;
    padding: 15px 5px;
    background: #f7f9ff;
    border-radius: 10px;
  }
  strong {
    display: block;
    color: #5064ad;
    font-size: 21px;
  }
  small {
    font-size: 10px;
    padding-left: 3px;
    font-weight: 400;
  }
  span {
    display: block;
    color: #a0abc3;
    font-size: 10px;
    margin-top: 7px;
  }
}
.report-detail-link {
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #6f87d8;
  font-size: 12px;
}
.performance {
  margin: 20px 0 16px;
  font-size: 12px;
  line-height: 1.9;
  color: #697999;
}
.advice-item {
  display: flex;
  gap: 10px;
  margin: 12px 0;
  > span {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 7px;
    background: #f2f4ff;
    color: #8398e7;
    font-size: 10px;
    flex-shrink: 0;
  }
  p {
    margin: 2px 0 0;
    font-size: 12px;
    line-height: 1.8;
    color: #8795b1;
  }
}
.report-limitation {
  color: #afb6c9;
  font-size: 10px;
  line-height: 1.7;
  margin-top: 16px;
}
.report-history {
  margin-top: 28px;
  h3 {
    font-size: 15px;
  }
  .section-heading a {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: #8b9bca;
  }
  > button {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    margin-top: 10px;
    border-radius: 10px;
    background: white;
    border: 1px solid var(--line);
    text-align: left;
    cursor: pointer;
    &.selected {
      border-color: #b4c3ff;
    }
  }
  strong {
    color: #65759a;
    font-size: 12px;
    display: block;
  }
  small {
    display: block;
    color: #a9b2c8;
    font-size: 10px;
    margin-top: 5px;
  }
  > button > svg {
    color: #a9b7db;
  }
}
.history-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #edf2ff;
  color: #7796ed;
}
.history-badge {
  margin-left: auto;
  font-size: 11px;
  color: #c6a045;
  background: #fff8df;
  padding: 5px 9px;
  border-radius: 12px;
}
.aggregation-note {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-top: 24px;
  padding: 13px 15px;
  border-radius: 10px;
  background: #f0f3fa;
  color: #9ba7c1;
  > svg {
    flex-shrink: 0;
  }
  p {
    margin: 0;
    font-size: 11px;
    line-height: 1.8;
  }
}
@media (max-width: 800px) {
  .tutor-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .tutor-panel {
    padding: 20px 14px;
  }
  .tutor-intro h2 {
    font-size: 21px;
  }
  .latest-report,
  .mentor-advice {
    padding: 18px;
  }
}
</style>
