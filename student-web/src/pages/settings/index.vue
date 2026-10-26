<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import { useToast } from "@/shared/composables/useToast";
import AppIcon from "@/shared/components/AppIcon.vue";
import PasswordChangeForm from "./components/PasswordChangeForm.vue";
const router = useRouter();
const auth = useAuthStore();
const { toast } = useToast();
const error = ref("");
const busy = ref(false);
const forced = computed(() => auth.user?.mustChangePassword);
async function logout() {
  busy.value = true;
  error.value = "";
  try {
    const revoked = await auth.logout();
    await router.replace("/login");
    if (!revoked) toast("已退出当前浏览器，服务器注销暂未确认。");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "退出失败，请重试";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">账号与安全</p>
        <h1>账号设置</h1>
        <p>守护账号安全，安心探索每一天。</p>
      </div>
      <RouterLink v-if="!forced" class="btn btn-outline" to="/profile"
        ><AppIcon name="arrow-left" :size="15" />我的空间</RouterLink
      >
    </div>
    <p v-if="forced" class="notice" role="alert">
      这是你的首次登录，请先修改初始密码。完成后需要使用新密码重新登录。
    </p>
    <div class="settings-grid">
      <section class="card">
        <div class="section-heading">
          <h2>修改登录密码</h2>
          <AppIcon name="lock" class="muted" />
        </div>
        <PasswordChangeForm />
      </section>
      <aside class="page-stack">
        <section class="card">
          <h3>当前账号</h3>
          <dl>
            <div>
              <dt>姓名</dt>
              <dd>{{ auth.user?.name }}</dd>
            </div>
            <div>
              <dt>学校</dt>
              <dd>{{ auth.user?.schoolName || auth.user?.schoolId }}</dd>
            </div>
            <div>
              <dt>手机号</dt>
              <dd>{{ auth.user?.phoneVerified ? "已完成验证" : "未验证" }}</dd>
            </div>
            <div>
              <dt>登录状态</dt>
              <dd>{{ isPreview ? "设计预览" : "已登录" }}</dd>
            </div>
          </dl>
          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
          <button
            class="btn btn-danger logout-button"
            :disabled="busy"
            @click="logout"
          >
            <AppIcon name="logout" :size="17" />{{
              busy ? "正在退出…" : "退出登录"
            }}
          </button>
        </section>
        <section class="notice">
          <AppIcon name="shield" :size="18" />
          <p>
            请妥善保管学习账号。需要修改学校、班级或个人资料时，可以联系你的老师。
          </p>
        </section>
      </aside>
    </div>
  </div>
</template>
<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 24px;
}
.section-heading {
  margin-bottom: 27px;
}
dl {
  margin: 0 0 22px;
}
dl > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}
dt {
  color: var(--muted);
}
dd {
  margin: 0;
  text-align: right;
}
.logout-button {
  width: 100%;
}
.notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.notice svg {
  flex-shrink: 0;
  margin-top: 3px;
}
.notice p {
  margin: 0;
  font-size: 12px;
}
@media (max-width: 767px) {
  .settings-grid {
    grid-template-columns: 1fr;
    gap: 22px;
  }
  .page-heading > .btn {
    font-size: 11px;
    padding: 10px;
  }
}
</style>
