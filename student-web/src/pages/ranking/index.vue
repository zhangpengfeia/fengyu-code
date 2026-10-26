<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import { getRanking } from "@/features/learning/api";
import type { RankingEntry } from "@/features/learning/types";
import RankingPodium from "./components/RankingPodium.vue";
import RankingList from "./components/RankingList.vue";

const auth = useAuthStore();
const entries = ref<RankingEntry[]>([]);
const loading = ref(true);
const error = ref("");
const volume = ref<number>();
const query = ref("");
const filteredEntries = computed(() =>
  entries.value.filter((entry) => entry.name.includes(query.value.trim())),
);
const currentEntry = computed(() =>
  entries.value.find((entry) => entry.id === auth.user?.id),
);
let requestVersion = 0;
async function load() {
  const version = ++requestVersion;
  loading.value = true;
  error.value = "";
  try {
    const result = await getRanking({
      classId: auth.user?.classId,
      volume: volume.value,
    });
    if (version === requestVersion) entries.value = result;
  } catch (cause) {
    if (version === requestVersion)
      error.value =
        cause instanceof Error ? cause.message : "排行榜加载失败，请稍后重试。";
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}
watch([volume, () => auth.user?.classId], load, { immediate: true });
onBeforeUnmount(() => {
  requestVersion += 1;
});
</script>

<template>
  <div class="page-stack ranking-page">
    <div class="page-actions">
      <button
        class="btn btn-soft refresh-btn"
        :disabled="loading"
        @click="load"
      >
        <AppIcon name="refresh" :size="16" />刷新
      </button>
    </div>
    <div class="ranking-layout">
      <section class="leaderboard-card card">
        <div class="board-heading">
          <div class="board-title">
            <span class="trophy-icon"
              ><AppIcon name="trophy" :size="23"
            /></span>
            <div>
              <h2>成长排行榜</h2>
              <p>
                {{ auth.user?.className || "我的班级"
                }}<span v-if="isPreview"> · 示例数据</span>
              </p>
            </div>
          </div>
          <label class="volume-select"
            ><span class="sr-only">筛选册次</span
            ><select v-model="volume">
              <option :value="undefined">全部册次</option>
              <option v-for="item in 12" :key="item" :value="item">
                第 {{ item }} 册
              </option></select
            ><AppIcon name="chevron-down" :size="13"
          /></label>
        </div>
        <div class="board-tabs">
          <button class="active" aria-pressed="true">班级榜</button
          ><button disabled title="校榜暂未向学生开放">
            <AppIcon name="lock" :size="12" />校园榜</button
          ><span>每一份努力都值得被看见</span>
        </div>
        <AsyncState
          :loading="loading"
          :error="error"
          :empty="entries.length === 0"
          empty-title="班级榜单还在等待第一颗星"
          @retry="load"
        >
          <RankingPodium v-if="!query.trim()" :entries="entries.slice(0, 3)" />
          <div class="ranking-search">
            <div class="list-caption">
              <h3>探索者们</h3>
              <span>{{ entries.length }} 位同学</span>
            </div>
            <label class="search-field"
              ><AppIcon name="search" :size="15" /><input
                v-model="query"
                type="search"
                placeholder="搜索同学"
                aria-label="搜索同学"
            /></label>
          </div>
          <RankingList :entries="filteredEntries" :user-id="auth.user?.id" />
        </AsyncState>
      </section>
      <aside class="ranking-aside">
        <section class="my-rank card">
          <span class="little-label">我的成长坐标</span>
          <div class="my-rank-icon"><AppIcon name="star" :size="29" /></div>
          <h3>每一次努力，都算数</h3>
          <div class="my-numbers">
            <div>
              <strong>{{
                loading || error ? "—" : (currentEntry?.rank ?? "—")
              }}</strong
              ><span>当前班级排名</span>
            </div>
            <div>
              <strong>{{
                loading || error ? "—" : (currentEntry?.score ?? "—")
              }}</strong
              ><span>当前学习积分</span>
            </div>
          </div>
          <p v-if="!loading && !error && !currentEntry" class="my-tip">
            你的积分记录暂未出现在当前榜单中。
          </p>
          <p v-else class="my-tip">和昨天的自己比，今天又进步了一点点。</p>
          <RouterLink to="/courses" class="btn btn-primary"
            >继续探索课程<AppIcon name="arrow-right" :size="15"
          /></RouterLink>
        </section>
        <section class="kindness-card">
          <span class="kindness-star">✧</span>
          <div class="kindness-symbol">
            <AppIcon name="sparkles" :size="25" />
          </div>
          <h3>每个人都有自己的光</h3>
          <p>
            有的同学善于观察，有的同学敢于尝试。发现自己的闪光点，一起成长就很棒。
          </p>
        </section>
        <div class="ranking-note">
          <AppIcon name="help" :size="15" />
          <p>榜单按当前筛选范围的学习积分排序。校园榜暂未向学生开放。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.heading-spark {
  font-size: 24px;
  color: #f2bc64;
  margin-left: 8px;
}
.refresh-btn {
  align-self: flex-end;
}
.ranking-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 278px;
  gap: 23px;
  align-items: start;
}
.leaderboard-card {
  padding: 26px;
  min-width: 0;
}
.board-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
}
.board-title {
  display: flex;
  align-items: center;
  gap: 11px;
}
.trophy-icon {
  height: 43px;
  width: 43px;
  display: grid;
  place-items: center;
  color: #edb54d;
  background: #fff5db;
  border-radius: 14px;
}
.board-title h2 {
  font-size: 19px;
  margin: 0 0 7px;
}
.board-title p {
  font-size: 10px;
  color: var(--muted);
  margin: 0;
}
.volume-select {
  position: relative;
  display: flex;
  align-items: center;
}
.volume-select select {
  appearance: none;
  color: #8391b0;
  padding: 9px 29px 9px 12px;
  border: 1px solid var(--line);
  background: #fcfdff;
  border-radius: 9px;
  font-size: 11px;
  cursor: pointer;
}
.volume-select > svg {
  position: absolute;
  right: 9px;
  pointer-events: none;
  color: #abb6cd;
}
.board-tabs {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 24px;
  padding: 10px 0 15px;
  border-bottom: 1px solid var(--line);
}
.board-tabs > button {
  border: 0;
  color: #9ca6bf;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 17px;
  border-radius: 9px;
  font-size: 11px;
}
.board-tabs > button.active {
  color: #6086f1;
  background: #edf2ff;
  font-weight: 700;
}
.board-tabs > button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.board-tabs > span {
  font-size: 9px;
  color: #b3bbcf;
  margin-left: auto;
}
.ranking-search {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
  padding: 25px 0 20px;
}
.list-caption {
  display: flex;
  align-items: center;
  gap: 10px;
}
.list-caption h3 {
  margin: 0;
  font-size: 13px;
}
.list-caption > span {
  font-size: 10px;
  color: #a5b0c7;
}
.search-field {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 151px;
  border: 1px solid var(--line);
  padding: 8px 10px;
  border-radius: 9px;
  color: #a8b3cb;
  background: #fcfdff;
}
.search-field input {
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--ink);
  font-size: 10px;
  outline: none;
}
.search-field:focus-within {
  border-color: #9eb5fa;
}
.search-field input::placeholder {
  color: #b3bdd1;
}
.ranking-aside {
  display: flex;
  flex-direction: column;
  gap: 19px;
}
.my-rank {
  padding: 25px 21px;
  text-align: center;
}
.little-label {
  font-size: 10px;
  color: var(--muted);
}
.my-rank-icon {
  height: 67px;
  width: 67px;
  margin: 26px auto 17px;
  border: 7px solid #fff5db;
  box-shadow: 0 0 0 1px #fff5db;
  background: #ffdfa0;
  color: #fff;
  border-radius: 23px;
  transform: rotate(-7deg);
  display: grid;
  place-items: center;
}
.my-rank h3 {
  font-size: 15px;
  margin: 0;
}
.my-numbers {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  margin: 29px 0 17px;
}
.my-numbers > div {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.my-numbers > div:first-child {
  border-right: 1px solid var(--line);
}
.my-numbers strong {
  font-size: 28px;
  color: #6486e1;
}
.my-numbers span {
  font-size: 10px;
  color: #9da8bf;
}
.my-tip {
  font-size: 10px;
  color: #acb4c7;
  line-height: 1.8;
  margin: 20px 0;
}
.my-rank > .btn {
  width: 100%;
  font-size: 11px;
}
.kindness-card {
  position: relative;
  padding: 26px;
  overflow: hidden;
  border: 1px solid #e8e1fa;
  border-radius: 19px;
  background: linear-gradient(135deg, #efeaff, #f7edff);
}
.kindness-symbol {
  color: #b09bcf;
}
.kindness-card h3 {
  font-size: 14px;
  color: #8c7cab;
  margin: 17px 0 10px;
}
.kindness-card p {
  font-size: 11px;
  line-height: 1.95;
  color: #a293b5;
  margin: 0;
  max-width: 207px;
}
.kindness-star {
  position: absolute;
  top: -5px;
  right: 10px;
  color: #dfd2f4;
  font-size: 74px;
  transform: rotate(20deg);
}
.ranking-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #a3adc2;
  padding: 0 3px;
}
.ranking-note > svg {
  flex-shrink: 0;
  margin-top: 3px;
}
.ranking-note p {
  margin: 0;
  font-size: 10px;
  line-height: 1.9;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@media (max-width: 1100px) {
  .ranking-layout {
    grid-template-columns: minmax(0, 1fr) 235px;
    gap: 17px;
  }
  .leaderboard-card {
    padding: 23px 20px;
  }
  .board-tabs > span {
    display: none;
  }
  .my-rank {
    padding: 23px 16px;
  }
  .kindness-card {
    padding: 23px;
  }
}
@media (max-width: 850px) {
  .ranking-layout {
    grid-template-columns: 1fr;
  }
  .ranking-aside {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }
  .my-rank {
    grid-row: span 2;
  }
  .kindness-card p {
    max-width: none;
  }
}
@media (max-width: 550px) {
  .leaderboard-card {
    padding: 20px 16px 14px;
  }
  .board-title h2 {
    font-size: 17px;
  }
  .board-title {
    gap: 9px;
  }
  .trophy-icon {
    height: 37px;
    width: 37px;
    border-radius: 12px;
  }
  .volume-select select {
    padding: 8px 25px 8px 9px;
    font-size: 10px;
  }
  .board-tabs {
    margin-top: 17px;
  }
  .ranking-search {
    padding: 24px 0 19px;
  }
  .list-caption {
    gap: 7px;
  }
  .list-caption > span {
    font-size: 9px;
  }
  .search-field {
    width: 131px;
  }
  .heading-spark {
    display: none;
  }
  .ranking-aside {
    display: flex;
    flex-direction: column;
  }
  .my-rank-icon {
    margin-top: 22px;
  }
  .my-numbers {
    max-width: 260px;
    margin-left: auto;
    margin-right: auto;
  }
  .my-rank > .btn {
    max-width: 260px;
  }
  .kindness-card {
    padding: 23px;
  }
  .kindness-card h3 {
    margin-top: 13px;
  }
  .refresh-btn {
    font-size: 11px;
    min-height: 34px;
    padding: 7px 10px;
  }
}
</style>
