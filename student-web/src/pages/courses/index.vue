<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AsyncState from "@/shared/components/AsyncState.vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import CourseCard from "@/features/courses/components/CourseCard.vue";
import { subjects } from "@/features/courses/types";
import { useCourseCatalog } from "@/features/courses/useCourseCatalog";
import CourseFilters from "./components/CourseFilters.vue";

const route = useRoute();
const router = useRouter();
const { courses, loading, error, load } = useCourseCatalog();
const subject = computed({
  get: () =>
    subjects.some((item) => item.code === route.query.subject)
      ? String(route.query.subject)
      : "ALL",
  set: (value: string) => {
    void router.replace({ query: value === "ALL" ? {} : { subject: value } });
  },
});
const keyword = ref("");
const visibleCourses = computed(() =>
  courses.value.filter(
    (course) =>
      (subject.value === "ALL" || course.subject === subject.value) &&
      `${course.name} ${course.description} ${course.ability ?? ""}`
        .toLowerCase()
        .includes(keyword.value.trim().toLowerCase()),
  ),
);
watch(
  () => route.query.subject,
  () => {
    keyword.value = "";
  },
);
</script>

<template>
  <div class="page-stack">
    <div class="page-actions">
      <span class="catalog-tag"
        ><AppIcon name="book" :size="16" />
        {{ courses.length }} 门探索课程</span
      >
    </div>

    <CourseFilters
      v-model:subject="subject"
      v-model:keyword="keyword"
    /><AsyncState
      :loading="loading"
      :error="error"
      :empty="!visibleCourses.length"
      :empty-title="courses.length ? '没有找到匹配的课程' : '还没有开放的课程'"
      @retry="load"
      ><div class="catalog-grid">
        <CourseCard
          v-for="course in visibleCourses"
          :key="course.id"
          :course="course"
        /></div
    ></AsyncState>
  </div>
</template>

<style scoped>
.catalog-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}
.catalog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}
@media (max-width: 1100px) {
  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .catalog-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }
}
</style>
