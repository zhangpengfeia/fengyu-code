<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
import { subjects } from "@/features/courses/types";
</script>

<template>
  <div class="subject-shortcuts">
    <RouterLink
      v-for="subject in subjects.slice(1)"
      :key="subject.code"
      :to="{ path: '/courses', query: { subject: subject.code } }"
      class="subject-shortcut"
      :class="`subject-${subject.tone}`"
      ><span class="subject-icon"
        ><AppIcon :name="subject.icon" :size="23" /></span
      ><span
        ><strong>{{ subject.name }}</strong
        ><small>{{
          {
            MATH: "发现思维的乐趣",
            READING: "读懂更大的世界",
            SCIENCE: "让好奇自由生长",
            CREATIVE: "点亮每一个灵感",
          }[subject.code as "MATH" | "READING" | "SCIENCE" | "CREATIVE"]
        }}</small></span
      ><AppIcon name="chevron-right" :size="15"
    /></RouterLink>
  </div>
</template>

<style scoped lang="less">
.subject-shortcuts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 13px;
}
.subject-shortcut {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 17px 14px;
  border-radius: 17px;
  border: 1px solid #f0e9df;
  background: #fffaf1;
  text-decoration: none;
  color: #d79732;
  transition: transform 0.2s;
  > svg {
    margin-left: auto;
    opacity: 0.5;
  }
  strong {
    display: block;
    font-size: 12px;
    color: var(--ink);
    margin-bottom: 5px;
  }
  small {
    display: block;
    font-size: 9px;
    color: var(--muted);
    white-space: nowrap;
  }
  &:hover {
    transform: translateY(-3px);
  }
}
.subject-icon {
  height: 38px;
  width: 38px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #fff0d0;
}
.subject-purple {
  color: #9c76dd;
  background: #f8f4ff;
  border-color: #eee6fc;
  .subject-icon {
    background: #eee4ff;
  }
}
.subject-green {
  color: #46a88f;
  background: #f0fbf6;
  border-color: #e0f2e9;
  .subject-icon {
    background: #dff4e8;
  }
}
.subject-pink {
  color: #d778a2;
  background: #fff4f8;
  border-color: #f9e5ee;
  .subject-icon {
    background: #fce0ec;
  }
}
@media (max-width: 1100px) {
  .subject-shortcuts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 400px) {
  .subject-shortcut {
    padding: 14px 11px;
    gap: 8px;
    > svg {
      display: none;
    }
  }
  .subject-icon {
    width: 34px;
    height: 34px;
  }
  .subject-shortcut small {
    font-size: 8px;
  }
}
</style>
