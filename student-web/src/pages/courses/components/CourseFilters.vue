<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import { subjects } from "@/features/courses/types";

defineProps<{ subject: string; keyword: string }>();
const emit = defineEmits<{
  "update:subject": [value: string];
  "update:keyword": [value: string];
}>();
</script>

<template>
  <div class="course-filters">
    <div class="subject-tabs" aria-label="学科筛选">
      <button
        v-for="item in subjects"
        :key="item.code"
        type="button"
        :class="{ active: subject === item.code }"
        :aria-pressed="subject === item.code"
        @click="emit('update:subject', item.code)"
      >
        <AppIcon :name="item.icon" :size="16" />{{ item.name }}
      </button>
    </div>
    <label class="search-field"
      ><AppIcon name="search" :size="17" /><input
        :value="keyword"
        type="search"
        placeholder="搜索课程或能力"
        aria-label="搜索课程"
        @input="
          emit('update:keyword', ($event.target as HTMLInputElement).value)
        "
    /></label>
  </div>
</template>

<style scoped lang="less">
.course-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 17px;
  flex-wrap: wrap;
}
.subject-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  button {
    display: flex;
    align-items: center;
    gap: 7px;
    border: 0;
    background: transparent;
    color: #8490ac;
    font: inherit;
    font-size: 11px;
    padding: 11px 13px;
    border-radius: 10px;
    cursor: pointer;
    white-space: nowrap;
    &:hover {
      background: #edf0fc;
    }
    &.active {
      background: var(--primary);
      color: #fff;
      box-shadow: 0 4px 10px #305bff20;
    }
  }
}
.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 11px;
  padding: 10px 13px;
  color: #9ca7bc;
  input {
    outline: none;
    border: 0;
    min-width: 0;
    width: 155px;
    background: transparent;
    font: inherit;
    font-size: 11px;
    color: var(--ink);
    &::placeholder {
      color: #a6afc4;
    }
  }
  &:focus-within {
    border-color: #8fa7ff;
    box-shadow: 0 0 0 3px #305bff0a;
  }
}
@media (max-width: 700px) {
  .course-filters {
    gap: 14px;
  }
  .subject-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    width: 100%;
    padding-bottom: 4px;
    button {
      font-size: 11px;
      padding: 10px 12px;
      flex-shrink: 0;
    }
  }
  .search-field {
    width: 100%;
    box-sizing: border-box;
    input {
      width: 100%;
    }
  }
}
</style>
