<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import { isPreview } from "@/shared/config";

const week = Array.from({ length: 7 }, (_, index) => {
  const date = new Date();
  date.setDate(date.getDate() - 6 + index);
  return {
    label: ["日", "一", "二", "三", "四", "五", "六"][date.getDay()],
    day: date.getDate(),
    today: index === 6,
    completed: isPreview && index >= 3 && index < 6,
  };
});
</script>

<template>
  <section class="rhythm-card card">
    <div class="rhythm-title">
      <span class="rhythm-icon"><AppIcon name="calendar" :size="19" /></span>
      <div>
        <h3>学习日历</h3>
      </div>
    </div>
    <div class="week-row" aria-label="最近七天">
      <div
        v-for="day in week"
        :key="day.label"
        class="week-day"
        :class="{ completed: day.completed, today: day.today }"
        :aria-label="`${day.day}日${day.today ? '，今天' : ''}${day.completed ? '，预览学习记录' : ''}`"
      >
        <span>{{ day.label }}</span
        ><span class="day-circle"
          ><AppIcon v-if="day.completed" name="check" :size="13" /><span
            v-else
            >{{ day.day }}</span
          ></span
        >
      </div>
    </div>
    <div class="rhythm-footer">
      <span class="tiny-star">✦</span>
      <p>
        {{
          isPreview
            ? "预览示例：已探索 3 天，继续保持！"
            : "学习日历暂未开放，先去课程里探索吧。"
        }}
      </p>
    </div>
  </section>
</template>

<style scoped lang="less">
.rhythm-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 273px;
  padding: 24px !important;
}
.rhythm-title {
  display: flex;
  gap: 11px;
  align-items: center;
  h3 {
    font-size: 13px;
    margin: 0 0 7px;
  }
  p {
    font-size: 10px;
    color: var(--muted);
    margin: 0;
  }
}
.rhythm-icon {
  display: grid;
  place-items: center;
  width: 35px;
  height: 35px;
  border-radius: 11px;
  background: #f0edff;
  color: #8b75db;
}
.week-row {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin-top: 24px;
}
.week-day {
  display: flex;
  flex-direction: column;
  gap: 11px;
  align-items: center;
  color: #9ba5be;
  font-size: 10px;
}
.day-circle {
  height: 29px;
  width: 29px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #f7f8fc;
  font-size: 10px;
}
.completed .day-circle {
  background: #edf1ff;
  color: #5575f5;
}
.today .day-circle {
  background: #4269fa;
  color: #fff;
  box-shadow: 0 4px 10px #4269fa30;
}
.rhythm-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid var(--line);
  margin-top: 24px;
  padding-top: 16px;
  p {
    font-size: 10px;
    color: var(--muted);
    margin: 0;
    line-height: 1.7;
  }
}
.tiny-star {
  color: #eab049;
  font-size: 18px;
}
@media (max-width: 850px) {
  .rhythm-card {
    min-height: 220px;
  }
  .week-row {
    margin-top: 19px;
  }
  .rhythm-footer {
    margin-top: 17px;
  }
}
</style>
