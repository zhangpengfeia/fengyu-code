<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import { courseImage, subjectName, type Course } from "../types";

defineProps<{ course: Course }>();
</script>

<template>
  <RouterLink
    :to="`/courses/${course.id}`"
    class="course-card"
    :class="`tone-${course.tone ?? course.subject.toLowerCase()}`"
  >
    <div class="course-art">
      <span class="course-volume">第 {{ course.volume }} 册</span>
      <span class="art-orbit" aria-hidden="true"></span>
      <img :src="courseImage(course.volume)" alt="" loading="lazy" />
      <span class="art-spark spark-one" aria-hidden="true">✦</span>
      <span class="art-spark spark-two" aria-hidden="true">✧</span>
      <span
        v-if="course.progress !== undefined && course.progress > 0"
        class="learning-tag"
        >正在学习</span
      >
    </div>
    <div class="course-copy">
      <span class="subject-label"
        >{{ subjectName(course.subject) }} <span>·</span>
        {{ course.stage || "探索课程" }}</span
      >
      <h3>{{ course.name }}</h3>
      <p>{{ course.description || "一步步探索新知识，发现自己的闪光点。" }}</p>
      <div v-if="course.progress !== undefined" class="course-progress">
        <div
          class="progress-track"
          role="progressbar"
          :aria-valuenow="course.progress"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-label="`${course.name}学习进度`"
        >
          <span :style="{ width: `${course.progress}%` }"></span>
        </div>
        <span>{{ course.progress }}%</span>
      </div>
      <div class="course-footer">
        <span>{{ course.progress ? "继续探索" : "查看课程" }}</span
        ><span class="arrow"><AppIcon name="arrow-right" :size="17" /></span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped lang="less">
.course-card {
  --course-accent: #4772ff;
  --course-soft: #e8efff;
  display: block;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: #fff;
  text-decoration: none;
  color: var(--ink);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 14px 35px #2539790d;
  }
  &:focus-visible {
    outline: 3px solid var(--primary);
    outline-offset: 4px;
  }
}
.tone-purple,
.tone-reading {
  --course-accent: #8b67dd;
  --course-soft: #eee7ff;
}
.tone-green,
.tone-science {
  --course-accent: #36ad99;
  --course-soft: #e0f4ea;
}
.tone-orange,
.tone-math {
  --course-accent: #ed9c40;
  --course-soft: #fff0dc;
}
.tone-pink,
.tone-creative {
  --course-accent: #dc81a8;
  --course-soft: #fce6ef;
}
.course-art {
  height: 152px;
  background:
    radial-gradient(ellipse at 70% 105%, #fff9 0 30%, transparent 31%),
    linear-gradient(125deg, var(--course-soft), #fafaff);
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  img {
    height: 128px;
    width: 144px;
    object-fit: contain;
    position: relative;
    filter: drop-shadow(0 10px 9px #63709926);
  }
}
.course-volume {
  position: absolute;
  top: 16px;
  left: 18px;
  color: var(--course-accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
}
.art-orbit {
  width: 190px;
  height: 66px;
  border: 1px solid #ffffffc4;
  position: absolute;
  border-radius: 50%;
  transform: rotate(-18deg);
}
.art-spark {
  position: absolute;
  color: var(--course-accent);
  opacity: 0.7;
}
.spark-one {
  left: 22%;
  top: 43%;
  font-size: 21px;
}
.spark-two {
  right: 21%;
  bottom: 20%;
  font-size: 15px;
}
.learning-tag {
  position: absolute;
  bottom: 13px;
  right: 14px;
  border-radius: 7px;
  color: var(--course-accent);
  background: #ffffffd6;
  padding: 4px 8px;
  font-size: 10px;
  font-weight: 700;
}
.course-copy {
  padding: 19px 21px 16px;
  h3 {
    margin: 8px 0 7px;
    font-size: 16px;
    line-height: 1.5;
  }
  p {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.8;
    min-height: 43px;
  }
}
.subject-label {
  font-size: 10px;
  color: var(--course-accent);
  font-weight: 700;
  span {
    margin: 0 5px;
    color: #c7cddd;
  }
}
.course-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  color: var(--muted);
  font-size: 10px;
}
.progress-track {
  height: 4px;
  background: #f0f2f9;
  flex: 1;
  border-radius: 5px;
  overflow: hidden;
  span {
    display: block;
    height: 100%;
    background: var(--course-accent);
    border-radius: 5px;
  }
}
.course-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 15px;
  font-size: 11px;
  color: var(--course-accent);
  font-weight: 700;
}
.arrow {
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: var(--course-soft);
  display: grid;
  place-items: center;
}
@media (max-width: 600px) {
  .course-art {
    height: 136px;
  }
  .course-copy {
    padding: 16px 18px;
  }
  .course-card:hover {
    transform: none;
  }
}
</style>
