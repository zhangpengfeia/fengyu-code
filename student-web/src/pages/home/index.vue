<script setup lang="ts">
import { computed } from "vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import CourseCard from "@/features/courses/components/CourseCard.vue";
import { useCourseCatalog } from "@/features/courses/useCourseCatalog";
import SubjectShortcuts from "./components/SubjectShortcuts.vue";
import StudyRhythm from "./components/StudyRhythm.vue";
const { courses, loading, error, load } = useCourseCatalog();
const featuredCourses = computed(() => courses.value.slice(0, 3));
</script>
<template>
  <div class="page-stack home-page">
    <SubjectShortcuts />
    <section>
      <div class="section-heading">
        <h2>我的课程</h2>
        <RouterLink to="/courses" class="link-button"
          >全部课程<AppIcon name="arrow-right" :size="16"
        /></RouterLink>
      </div>
      <AsyncState
        :loading="loading"
        :error="error"
        :empty="!courses.length"
        empty-title="还没有开放的课程"
        @retry="load"
        ><div class="featured-grid">
          <CourseCard
            v-for="course in featuredCourses"
            :key="course.id"
            :course="course"
          /></div
      ></AsyncState>
    </section>
    <div class="home-support">
      <StudyRhythm />
      <RouterLink to="/companion" class="companion-entry card"
        ><MascotAvatar :size="56" />
        <div>
          <h2>智能学伴</h2>
          <p>游戏规则、学习问答与测评建议</p>
        </div>
        <AppIcon name="chevron-right" :size="20"
      /></RouterLink>
    </div>
  </div>
</template>
<style scoped>
.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}
.home-support {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}
.companion-entry {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 120px;
}
.companion-entry h2 {
  font-size: 17px;
  margin: 0 0 10px;
}
.companion-entry p {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
  line-height: 1.8;
}
.companion-entry > svg {
  margin-left: auto;
  color: var(--muted);
  flex-shrink: 0;
}
@media (max-width: 1100px) and (min-width: 768px) {
  .featured-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .featured-grid > :nth-child(n + 3) {
    display: none;
  }
  .home-support {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 767px) {
  .featured-grid,
  .home-support {
    grid-template-columns: 1fr;
    gap: 18px;
  }
}
</style>
