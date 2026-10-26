<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { agentRequest } from "@/shared/api/client";
import { getEvaluation } from "@/features/learning/api";

const route = useRoute();
const router = useRouter();
const busy = ref(false);
const error = ref("");
const status = ref("");
const requestId = ref(crypto.randomUUID());
const gameCode = computed(() => String(route.params.gameCode));
let disposed = false;
onBeforeUnmount(() => {
  disposed = true;
});

async function finish() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const result = await agentRequest<{ job_id: string }>({
      url: "/dev/mock/game-finish",
      method: "POST",
      data: { request_id: requestId.value, game_code: gameCode.value },
    });
    status.value = "本局结束事件已保存，后端正在生成示例评价…";
    for (let count = 0; count < 40 && !disposed; count++) {
      const report = await getEvaluation(result.job_id);
      if (report.status.startsWith("COMPLETED")) {
        await router.push(`/reports/${result.job_id}`);
        return;
      }
      if (report.status === "FAILED") throw new Error("评价任务失败，请检查后端日志。");
      await new Promise((resolve) => setTimeout(resolve, 300));
    }
    if (!disposed) status.value = "报告仍在后台处理，请进入学习报告查看。";
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "联调请求失败";
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <section class="card mock-game">
    <p class="eyebrow">Mock · 游戏结束联调</p>
    <h1>验证一局游戏的完整测评流程</h1>
    <p>当前游戏：{{ gameCode }}</p>
    <p>这是联调入口，不是真实游戏。点击下方按钮后，后端使用固定示例成绩创建本局数据，再通过评价任务生成报告。</p>
    <button class="btn btn-primary" :disabled="busy" @click="finish">{{ busy ? "正在生成评价…" : "模拟结束本局并生成评价" }}</button>
    <p v-if="status" role="status">{{ status }}</p>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <RouterLink to="/reports" class="btn btn-soft">查看学习报告</RouterLink>
  </section>
</template>

<style scoped>
.mock-game {
  max-width: 850px;
  padding: 32px;
}
.mock-game p {
  line-height: 1.9;
  color: var(--muted);
}
.mock-game h1 {
  font-size: 25px;
}
.mock-game .btn {
  margin: 15px 12px 10px 0;
}
</style>
