<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import type { RankingEntry } from "@/features/learning/types";

defineProps<{ entries: RankingEntry[]; userId?: string }>();
</script>

<template>
  <div class="ranking-list" role="table" aria-label="班级积分排名">
    <div class="list-heading" role="row">
      <span role="columnheader">排名</span
      ><span role="columnheader">探索者</span
      ><span role="columnheader">学习积分</span>
    </div>
    <div
      v-for="entry in entries"
      :key="entry.id"
      class="ranking-row"
      :class="{ 'is-me': entry.id === userId }"
      role="row"
    >
      <span class="rank-number" :class="{ top: entry.rank <= 3 }" role="cell">{{
        String(entry.rank).padStart(2, "0")
      }}</span
      ><span class="student-cell" role="cell"
        ><span
          class="student-avatar"
          :style="{ '--avatar-hue': `${(entry.rank * 47 + 198) % 360}` }"
          >{{ entry.name.slice(0, 1) }}</span
        ><span class="student-name"
          >{{ entry.name
          }}<span v-if="entry.id === userId" class="me-label">我</span></span
        ></span
      ><span class="score-cell" role="cell"
        ><AppIcon name="star" :size="14" />{{ entry.score
        }}<span>分</span></span
      >
    </div>
    <p v-if="!entries.length" class="no-result">
      没有找到这位同学，试试其他名字吧。
    </p>
  </div>
</template>

<style scoped>
.list-heading,
.ranking-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 105px;
  align-items: center;
  gap: 10px;
}
.list-heading {
  padding: 0 18px 14px;
  font-size: 10px;
  color: #a2acc1;
}
.list-heading > span:last-child {
  text-align: right;
}
.ranking-row {
  margin-bottom: 8px;
  padding: 14px 18px;
  background: #f9faff;
  border: 1px solid transparent;
  border-radius: 13px;
  min-height: 66px;
}
.ranking-row.is-me {
  background: #eef2ff;
  border-color: #dbe3ff;
}
.rank-number {
  font-size: 14px;
  color: #aab4cb;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}
.rank-number.top {
  color: #c5a06a;
}
.student-cell {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}
.student-avatar {
  height: 34px;
  width: 34px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: hsl(var(--avatar-hue) 75% 95%);
  color: hsl(var(--avatar-hue) 38% 65%);
  font-size: 13px;
  font-weight: 700;
}
.student-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #737f9d;
  min-width: 0;
  overflow-wrap: anywhere;
}
.me-label {
  font-size: 9px;
  padding: 2px 5px;
  background: #dce5ff;
  color: #7c97df;
  border-radius: 4px;
  flex-shrink: 0;
}
.score-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #8493c5;
  font-variant-numeric: tabular-nums;
}
.score-cell > svg {
  color: #e8bf70;
}
.score-cell > span {
  font-size: 10px;
  font-weight: 400;
  color: #b0b7cb;
  margin-left: 2px;
}
.is-me .student-name,
.is-me .score-cell {
  color: #557be3;
}
.no-result {
  text-align: center;
  color: var(--muted);
  padding: 28px 10px;
  font-size: 12px;
}
@media (max-width: 500px) {
  .list-heading,
  .ranking-row {
    grid-template-columns: 35px minmax(0, 1fr) 82px;
    gap: 7px;
  }
  .list-heading {
    padding-right: 12px;
    padding-left: 12px;
  }
  .ranking-row {
    padding: 12px;
  }
  .student-cell {
    gap: 8px;
  }
  .student-avatar {
    width: 30px;
    height: 30px;
    border-radius: 10px;
  }
  .score-cell {
    font-size: 14px;
    gap: 4px;
  }
  .student-name {
    font-size: 11px;
  }
}
</style>
