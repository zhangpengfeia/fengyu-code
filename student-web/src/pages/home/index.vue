<script setup lang="ts">
import { computed } from "vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import AsyncState from "@/shared/components/AsyncState.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import CourseCard from "@/features/courses/components/CourseCard.vue";
import { useCourseCatalog } from "@/features/courses/useCourseCatalog";
import { isPreview } from "@/shared/config";
import LearningHero from "./components/LearningHero.vue";
import SubjectShortcuts from "./components/SubjectShortcuts.vue";
import StudyRhythm from "./components/StudyRhythm.vue";

const { courses, loading, error, load } = useCourseCatalog();
const featuredCourses = computed(() => courses.value.slice(0, 3));
</script>

<template>
  <div class="page-stack home-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">开启今日探索</p>
        <h1>今天，也一起探索吧 <span class="greeting-spark">✦</span></h1>
        <p class="muted">每一次好奇，都是成长的开始。</p>
      </div>
      <RouterLink to="/reports" class="report-link"
        ><AppIcon name="chart" :size="16" /> 我的学习报告
        <AppIcon name="chevron-right" :size="14"
      /></RouterLink>
    </div>
    <div class="hero-grid"><LearningHero /><StudyRhythm /></div>
    <SubjectShortcuts />
    <section class="courses-section">
      <div class="section-heading">
        <div>
          <h2>{{ isPreview ? "继续你的探索" : "我的探索课程" }}</h2>
          <p class="muted">
            {{
              isPreview
                ? "知识星球已就绪，出发去看看吧"
                : "这里展示学校为你开放的学习内容"
            }}
          </p>
        </div>
        <RouterLink to="/courses" class="text-link"
          >全部课程 <AppIcon name="arrow-right" :size="16"
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
    <RouterLink to="/companion" class="companion-strip"
      ><MascotAvatar :size="57" />
      <div class="companion-strip-copy">
        <span class="companion-label">你的专属智能学伴</span>
        <h3>有个问题，想一起找答案？</h3>
        <p>游戏规则、解题思路、学习建议……随时来和我聊聊。</p>
      </div>
      <span class="companion-strip-button"
        >和学伴聊聊 <AppIcon name="arrow-right" :size="16" /></span
      ><span class="strip-spark" aria-hidden="true">✧</span></RouterLink
    >
  </div>
</template>

<style scoped lang="less">
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.85fr) minmax(280px, 1fr);
  gap: 20px;
}
.greeting-spark {
  color: #f4bb53;
  margin-left: 6px;
}
.report-link,
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--primary);
  text-decoration: none;
  font-size: 11px;
  font-weight: 650;
}
.report-link {
  padding: 11px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  white-space: nowrap;
}
.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}
.companion-strip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 19px;
  padding: 24px 28px;
  border: 1px solid #e6e7fb;
  border-radius: 22px;
  background: linear-gradient(100deg, #eceeff, #f5f4ff 58%, #f0f7ff);
  text-decoration: none;
  overflow: hidden;
  color: var(--ink);
}
.companion-label {
  font-size: 10px;
  color: #8872cb;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.companion-strip-copy h3 {
  font-size: 16px;
  margin: 7px 0;
}
.companion-strip-copy p {
  font-size: 11px;
  line-height: 1.8;
  color: var(--muted);
  margin: 0;
}
.companion-strip-button {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #e1e6fa;
  color: var(--primary);
  border-radius: 11px;
  padding: 12px 17px;
  margin-left: auto;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 700;
  position: relative;
  z-index: 1;
}
.strip-spark {
  position: absolute;
  right: 19%;
  top: -18px;
  font-size: 115px;
  color: #d9dcfb66;
  transform: rotate(12deg);
}
@media (max-width: 1100px) {
  .hero-grid {
    grid-template-columns: minmax(0, 1.7fr) minmax(250px, 1fr);
  }
}
@media (max-width: 850px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
  .featured-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .featured-grid > :nth-child(n + 3) {
    display: none;
  }
  .companion-strip {
    padding: 22px;
    gap: 14px;
  }
  .companion-strip-button {
    font-size: 0;
    gap: 0;
    padding: 11px;
  }
  .companion-strip-copy h3 {
    font-size: 14px;
  }
  .companion-strip-copy p {
    max-width: 270px;
  }
  .report-link {
    font-size: 0;
    gap: 0;
    padding: 10px;
    > svg:last-child {
      display: none;
    }
  }
}
@media (max-width: 560px) {
  .featured-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  .companion-strip {
    padding: 19px 17px;
    gap: 12px;
  }
  .companion-strip-copy p {
    font-size: 10px;
  }
  .companion-strip-copy h3 {
    font-size: 12px;
  }
  .companion-strip-button {
    padding: 9px;
  }
  .companion-label {
    font-size: 9px;
  }
  .strip-spark {
    display: none;
  }
}
</style>
