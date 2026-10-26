import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: "/login",
      name: "login",
      component: () => import("@/pages/login/index.vue"),
      meta: { title: "欢迎回来", public: true },
    },
    {
      path: "/",
      component: () => import("@/app/layouts/StudentLayout.vue"),
      children: [
        { path: "", redirect: "/home" },
        {
          path: "home",
          name: "home",
          component: () => import("@/pages/home/index.vue"),
          meta: { title: "学习首页" },
        },
        {
          path: "courses",
          name: "courses",
          component: () => import("@/pages/courses/index.vue"),
          meta: { title: "课程探索" },
        },
        {
          path: "courses/:id",
          name: "course-detail",
          component: () => import("@/pages/course-detail/index.vue"),
          meta: { title: "课程详情" },
        },
        {
          path: "reports",
          name: "reports",
          component: () => import("@/pages/reports/index.vue"),
          meta: { title: "学习报告" },
        },
        {
          path: "reports/:id",
          name: "report-detail",
          component: () => import("@/pages/report-detail/index.vue"),
          meta: { title: "测评详情" },
        },
        {
          path: "ranking",
          name: "ranking",
          component: () => import("@/pages/ranking/index.vue"),
          meta: { title: "成长排行榜" },
        },
        {
          path: "profile",
          name: "profile",
          component: () => import("@/pages/profile/index.vue"),
          meta: { title: "我的空间" },
        },
        {
          path: "settings",
          name: "settings",
          component: () => import("@/pages/settings/index.vue"),
          meta: { title: "账号设置" },
        },
        {
          path: "companion",
          name: "companion",
          component: () => import("@/pages/companion/index.vue"),
          meta: { title: "智能学伴" },
        },
      ],
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: () => import("@/pages/not-found/index.vue"),
      meta: { title: "页面未找到", public: true },
    },
  ],
});

router.beforeEach(async (to) => {
  document.title = `${to.meta.title || "学习空间"} · 枫语趣学`;
  const auth = useAuthStore();
  if (to.meta.public) return true;
  try {
    await auth.initialize();
  } catch {
    auth.clear();
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (!auth.isAuthenticated)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (!isPreview && auth.user?.mustChangePassword && to.name !== "settings")
    return { path: "/settings", query: { force: "1" } };
});
