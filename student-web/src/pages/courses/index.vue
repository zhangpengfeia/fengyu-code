<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AsyncState from "@/shared/components/AsyncState.vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import { assetUrl } from "@/shared/utils/assets";
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
    <div class="page-heading">
      <div>
        <p class="eyebrow">发现更大的世界</p>
        <h1>探索课程</h1>
        <p class="muted">找到你的兴趣，开启一段新的学习旅程。</p>
      </div>
      <span class="catalog-tag"
        ><AppIcon name="book" :size="16" />
        {{ courses.length }} 门探索课程</span
      >
    </div>
    <section class="catalog-banner">
      <div>
        <span>在玩中学，在学中成长</span>
        <h2>每一颗知识星球，<br class="mobile-break" />都藏着你的无限可能。</h2>
        <p>思考 · 表达 · 发现 · 创造</p>
      </div>
      <div class="banner-planets" aria-hidden="true">
        <span class="banner-ring"></span
        ><img :src="assetUrl('images/planet-3.webp')" alt="" /><span
          class="banner-spark"
          >✦</span
        >
      </div>
    </section>
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
    <p
      v-if="!loading && !error && visibleCourses.length"
      class="catalog-footer"
    >
      按照自己的节奏探索，每一小步都算数 <span>✧</span>
    </p>
  </div>
</template>

<style scoped lang="less">
.catalog-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #8d99b2;
  font-size: 11px;
  background: #fff;
  padding: 10px 13px;
  border: 1px solid var(--line);
  border-radius: 11px;
  white-space: nowrap;
}
.catalog-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  padding: 29px 32px;
  min-height: 166px;
  background: linear-gradient(105deg, #e9eeff, #f0edff 58%, #e4f1ff);
  > div:first-child {
    position: relative;
    z-index: 1;
  }
  span {
    font-size: 10px;
    color: #7c84bf;
    letter-spacing: 1px;
  }
  h2 {
    color: #3d4e99;
    font-size: 24px;
    line-height: 1.7;
    margin: 9px 0 12px;
    font-weight: 750;
  }
  p {
    font-size: 10px;
    color: #9299bb;
    letter-spacing: 4px;
    margin: 0;
  }
}
.mobile-break {
  display: none;
}
.banner-planets {
  position: absolute !important;
  right: 8%;
  height: 185px;
  width: 210px;
  display: grid;
  place-items: center;
  img {
    width: 172px;
    filter: drop-shadow(0 13px 13px #7181b826);
    transform: rotate(6deg);
  }
  .banner-spark {
    position: absolute;
    font-size: 30px;
    color: #beade3;
    right: 0;
    top: 24px;
  }
}
.banner-ring {
  position: absolute;
  width: 270px;
  height: 75px;
  border: 1px solid #ffffffc4;
  border-radius: 50%;
  transform: rotate(-25deg);
}
.catalog-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}
.catalog-footer {
  text-align: center;
  color: #a1aac0;
  font-size: 10px;
  margin: 6px 0 0;
  span {
    color: #d9b570;
    margin-left: 5px;
  }
}
@media (max-width: 950px) {
  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .banner-planets {
    right: 1%;
  }
}
@media (max-width: 600px) {
  .catalog-grid {
    grid-template-columns: 1fr;
    gap: 17px;
  }
  .catalog-banner {
    padding: 25px 23px;
    min-height: 185px;
    h2 {
      font-size: 20px;
      max-width: 240px;
    }
    .banner-planets {
      right: -27%;
      opacity: 0.55;
      img {
        width: 155px;
      }
    }
  }
  .mobile-break {
    display: block;
  }
  .catalog-tag {
    font-size: 0;
    padding: 10px;
    gap: 0;
  }
}
</style>
