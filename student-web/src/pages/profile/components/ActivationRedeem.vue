<script setup lang="ts">
import { ref } from "vue";
import { redeemActivationCode } from "@/features/profile/api";
import { isPreview } from "@/shared/config";
import { useToast } from "@/shared/composables/useToast";
const { toast } = useToast();
const code = ref("");
const error = ref("");
const busy = ref(false);
async function redeem() {
  error.value = "";
  if (isPreview) {
    toast("设计预览不会兑换激活码或修改授权。");
    return;
  }
  busy.value = true;
  try {
    await redeemActivationCode(code.value);
    code.value = "";
    toast("激活码兑换成功，可以前往课程页查看。");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "兑换失败";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <form class="activation-form" @submit.prevent="redeem">
    <label class="field"
      >课程激活码<input
        v-model.trim="code"
        required
        placeholder="输入老师提供的激活码"
        maxlength="100"
        autocomplete="off"
    /></label>
    <p v-if="error" role="alert" class="error-text">{{ error }}</p>
    <button class="btn btn-primary" :disabled="busy">
      {{ busy ? "正在兑换…" : "确认兑换" }}
    </button>
    <p class="muted">激活码仅可兑换一次，兑换后的资源由学校统一管理。</p>
  </form>
</template>
<style scoped>
.activation-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.activation-form > p {
  font-size: 11px;
  line-height: 1.8;
  margin: 0;
}
</style>
