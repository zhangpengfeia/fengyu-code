<template>
  <div class="app-container education-page">
    <div class="edu-welcome">
      <div>
        <span class="edu-eyebrow">教学管理工作台</span>
        <h1>{{ greeting }}，{{ store.nickName || store.name }}</h1>
        <p>在这里找到你的课件、游戏和教学管理工具。</p>
        <el-space
          ><el-tag effect="plain" v-for="role in store.roles" :key="role">{{
            labels[role] || role
          }}</el-tag></el-space
        >
      </div>
      <div class="welcome-orbit">◈</div>
    </div>
    <el-row :gutter="20" class="edu-gap"
      ><el-col :xs="24" :md="8" v-for="item in entries" :key="item.path"
        ><el-card shadow="hover" class="edu-entry"
          ><div :class="['entry-symbol', item.color]">{{ item.symbol }}</div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.description }}</p>
          <el-button type="primary" plain @click="router.push(item.path)"
            >进入{{ item.title }}
            <el-icon><Right /></el-icon></el-button></el-card></el-col></el-row
    ><el-card shadow="never" class="edu-gap edu-card"
      ><template #header>我的账号</template
      ><el-descriptions :column="2"
        ><el-descriptions-item label="登录账号">{{
          store.name
        }}</el-descriptions-item
        ><el-descriptions-item label="权限来源"
          >角色菜单与资源授权</el-descriptions-item
        ></el-descriptions
      ><el-button
        link
        type="primary"
        @click="router.push('/education-security')"
        >修改密码与验证手机号 →</el-button
      ></el-card
    >
  </div>
</template>
<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import useUserStore from "@/store/modules/user";
import { labels } from "@/api/education";
const store = useUserStore(),
  router = useRouter();
const greeting = computed(() =>
  new Date().getHours() < 12
    ? "上午好"
    : new Date().getHours() < 18
      ? "下午好"
      : "晚上好",
);
const items = [
  {
    path: "/education/games",
    title: "游戏管理",
    permission: "biz:game:list",
    symbol: "◈",
    color: "blue",
    description: "探索已授权的互动教学游戏，快速预览并进入游戏。",
  },
  {
    path: "/education/courseware",
    title: "课件管理",
    permission: "biz:courseware:list",
    symbol: "▤",
    color: "green",
    description: "按学科与册次组织教学内容，连接课堂互动资源。",
  },
  {
    path: "/education/users",
    title: "用户管理",
    permission: "biz:user:list",
    symbol: "◎",
    color: "purple",
    description: "维护学校账号与班级关系，让每个角色各司其职。",
  },
];
const entries = computed(() =>
  items.filter(
    (x) =>
      store.permissions.includes("*:*:*") ||
      store.permissions.includes(x.permission),
  ),
);
</script>
