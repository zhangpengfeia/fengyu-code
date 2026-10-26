<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { useToast } from "@/shared/composables/useToast";
import ActionList from "@/shared/components/ActionList.vue";
import ActionListItem from "@/shared/components/ActionListItem.vue";
import ActionDialog from "@/shared/components/ActionDialog.vue";
import PasswordChangeForm from "./components/PasswordChangeForm.vue";
import AccountDetails from "./components/AccountDetails.vue";
const router = useRouter();
const auth = useAuthStore();
const { toast } = useToast();
const error = ref("");
const busy = ref(false);
const panel = ref<"account" | "password" | null>(null);
const forced = computed(() => Boolean(auth.user?.mustChangePassword));
const panelVisible = computed({
  get: () => panel.value !== null,
  set: (value) => {
    if (!value && !forced.value) panel.value = null;
  },
});
watch(
  forced,
  (value) => {
    if (value) panel.value = "password";
  },
  { immediate: true },
);
async function logout() {
  busy.value = true;
  error.value = "";
  try {
    const revoked = await auth.logout();
    await router.replace("/login");
    if (!revoked) toast("已退出当前浏览器，服务器注销暂未确认。");
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "退出失败，请重试";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="page-stack">
    <p v-if="forced" class="notice" role="alert">
      这是你的首次登录，请先修改初始密码。完成后需要使用新密码重新登录。
    </p>
    <ActionList label="账号设置">
      <ActionListItem
        label="当前账号"
        icon="user"
        :value="auth.user?.name"
        :disabled="forced"
        @click="panel = 'account'"
      />
      <ActionListItem
        label="修改登录密码"
        icon="lock"
        tone="purple"
        @click="panel = 'password'"
      />
      <ActionListItem label="我的空间" icon="home" to="/profile" />
    </ActionList>
    <ActionList label="登录操作"
      ><ActionListItem
        :label="busy ? '正在退出…' : '退出登录'"
        icon="logout"
        danger
        :disabled="busy"
        @click="logout"
    /></ActionList>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <ActionDialog
      v-model:show="panelVisible"
      :title="panel === 'password' ? '修改登录密码' : '当前账号'"
      :dismissible="!forced"
    >
      <PasswordChangeForm v-if="panel === 'password'" />
      <AccountDetails v-else-if="panel === 'account'" />
    </ActionDialog>
  </div>
</template>
