<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { changePassword } from "@/features/auth/api";
import { isPreview } from "@/shared/config";
import { useToast } from "@/shared/composables/useToast";
const router = useRouter();
const auth = useAuthStore();
const { toast } = useToast();
const form = reactive({ old: "", next: "", confirm: "" });
const busy = ref(false);
const error = ref("");
async function submit() {
  error.value = "";
  const classes = [/[a-zA-Z]/, /[0-9]/, /[^a-zA-Z0-9]/].filter((pattern) =>
    pattern.test(form.next),
  ).length;
  if (form.next.length < 8 || form.next.length > 32 || classes < 2) {
    error.value = "新密码须为 8～32 位，包含字母、数字、符号中的至少两类。";
    return;
  }
  if (form.next !== form.confirm) {
    error.value = "两次输入的新密码不一致。";
    return;
  }
  if (form.next === form.old) {
    error.value = "请设置与原密码不同的新密码。";
    return;
  }
  if (isPreview) {
    toast("设计预览不会修改账号密码。");
    return;
  }
  busy.value = true;
  try {
    await changePassword(form.old, form.next);
    auth.clear();
    toast("密码已修改，请使用新密码重新登录。");
    await router.replace("/login");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "修改失败，请稍后重试";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <form class="password-change" @submit.prevent="submit">
    <label class="field"
      >原密码<input
        v-model="form.old"
        type="password"
        required
        autocomplete="current-password"
        placeholder="请输入当前密码"
        maxlength="100" /></label
    ><label class="field"
      >新密码<input
        v-model="form.next"
        type="password"
        required
        autocomplete="new-password"
        placeholder="设置一个新的安全密码"
        minlength="8"
        maxlength="32"
      /><small>8～32 位，包含字母、数字、符号中的至少两类。</small></label
    ><label class="field"
      >确认新密码<input
        v-model="form.confirm"
        type="password"
        required
        autocomplete="new-password"
        placeholder="再次输入新密码"
        minlength="8"
        maxlength="32"
    /></label>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <button class="btn btn-primary" :disabled="busy">
      {{ busy ? "正在更新…" : "更新密码" }}
    </button>
  </form>
</template>
<style scoped>
.password-change {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 460px;
}
.password-change > .btn {
  align-self: flex-start;
  min-width: 120px;
}
.password-change > p {
  margin: 0;
}
</style>
