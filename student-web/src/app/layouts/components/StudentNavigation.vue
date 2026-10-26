<script setup lang="ts">
import { useRoute } from "vue-router";
import AppIcon from "@/shared/components/AppIcon.vue";
defineProps<{ mobile?: boolean }>();
const route = useRoute();
const items = [
  { path: "/home", label: "学习首页", short: "首页", icon: "home" },
  { path: "/courses", label: "课程探索", short: "课程", icon: "book" },
  { path: "/reports", label: "学习报告", short: "报告", icon: "report" },
  { path: "/ranking", label: "成长排行榜", short: "排行", icon: "trophy" },
  { path: "/profile", label: "我的空间", short: "我的", icon: "user" },
];
</script>
<template>
  <nav
    :class="['student-nav', { mobile }]"
    :aria-label="mobile ? '底部导航' : '主导航'"
  >
    <RouterLink
      v-for="item in items"
      :key="item.path"
      :to="item.path"
      :class="{ active: route.path.startsWith(item.path) }"
      :aria-current="route.path.startsWith(item.path) ? 'page' : undefined"
      ><AppIcon :name="item.icon" :size="mobile ? 21 : 20" /><span>{{
        mobile ? item.short : item.label
      }}</span
      ><i v-if="!mobile && route.path.startsWith(item.path)"
    /></RouterLink>
  </nav>
</template>
<style scoped lang="less">
.student-nav {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
a {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 18px;
  color: #8390ad;
  font-size: 13px;
  font-weight: 500;
  border-radius: 13px;
  transition: background 0.15s;
}
a:hover {
  background: #f3f6ff;
}
a.active {
  background: #edf2ff;
  color: var(--primary);
  font-weight: 600;
}
i {
  margin-left: auto;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}
.mobile {
  flex-direction: row;
  justify-content: space-around;
  gap: 0;
}
.mobile a {
  flex-direction: column;
  gap: 5px;
  padding: 11px 0 8px;
  font-size: 10px;
  flex: 1;
  border-radius: 0;
}
.mobile a.active {
  background: transparent;
}
.mobile a.active svg {
  filter: drop-shadow(0 2px 5px #305bff22);
}
</style>
