<script setup lang="ts">
import AppIcon from "@/shared/components/AppIcon.vue";
</script>
<template>
  <main class="not-found">
    <span>404</span>
    <h1>页面未找到</h1>
    <p>这条路径还没有被探索，回到首页继续吧。</p>
    <RouterLink class="btn btn-primary" to="/home"
      ><AppIcon name="home" :size="17" />返回学习首页</RouterLink
    >
  </main>
</template>
<style scoped>
.not-found {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
}
.not-found > span {
  font-size: 100px;
  font-weight: 800;
  letter-spacing: -0.08em;
  color: #d5dfff;
}
.not-found h1 {
  font-size: 25px;
}
.not-found p {
  color: var(--muted);
  font-size: 14px;
  margin-bottom: 24px;
}
</style>
