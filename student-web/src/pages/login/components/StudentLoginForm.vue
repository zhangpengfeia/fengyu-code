<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getCaptcha } from "@/features/auth/api";
import { useAuthStore } from "@/features/auth/store";
import type { Captcha, LoginInput } from "@/features/auth/types";
import { isPreview, isMock } from "@/shared/config";
import AppIcon from "@/shared/components/AppIcon.vue";
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const form = reactive<LoginInput>({
  mode: "STUDENT",
  username: "",
  password: "",
  school_id: "",
  code: "",
  uuid: "",
});
const captcha = ref<Captcha>({ captchaEnabled: true });
const ready = ref(false);
const busy = ref(false);
const captchaBusy = ref(false);
const error = ref("");
const visible = ref(false);
const usernameLabel = computed(() =>
  form.mode === "STUDENT"
    ? "学号"
    : form.mode === "PHONE"
      ? "已验证手机号"
      : "账号",
);
async function refreshCaptcha() {
  if (isPreview) return;
  captchaBusy.value = true;
  try {
    captcha.value = await getCaptcha();
    form.uuid = captcha.value.uuid || "";
    form.code = "";
    ready.value = true;
    error.value = "";
  } catch (e) {
    ready.value = false;
    error.value = e instanceof Error ? e.message : "验证码加载失败";
  } finally {
    captchaBusy.value = false;
  }
}
async function submit() {
  if (isPreview) {
    await router.push("/home");
    return;
  }
  if (!ready.value || busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await auth.login(form);
    const redirect =
      typeof route.query.redirect === "string" ? route.query.redirect : "/home";
    await router.replace(
      redirect.startsWith("/") &&
        !redirect.startsWith("//") &&
        !redirect.startsWith("/login")
        ? redirect
        : "/home",
    );
  } catch (e) {
    const message = e instanceof Error ? e.message : "登录失败，请稍后重试";
    await refreshCaptcha();
    error.value = message;
  } finally {
    busy.value = false;
  }
}
onMounted(() => {
  void refreshCaptcha();
});
</script>
<template>
  <div class="student-login-form">
    <h2>登录</h2>

    <div v-if="isPreview" class="notice preview-notice">
      当前为设计预览，无需输入账号密码。<RouterLink
        class="btn btn-primary"
        to="/home"
        >进入设计预览<AppIcon name="arrow-right" :size="16"
      /></RouterLink>
    </div>
    <form v-else @submit.prevent="submit">
      <p v-if="isMock" class="notice" role="note">
        Mock 联调账号：学校 11000，学号 demo，密码 demo123。登录将请求本地后端。
      </p>
      <div class="segmented">
        <button
          v-for="item in [
            { value: 'STUDENT', label: '学号登录' },
            { value: 'ACCOUNT', label: '账号登录' },
            { value: 'PHONE', label: '手机号登录' },
          ]"
          :key="item.value"
          type="button"
          :class="{ active: form.mode === item.value }"
          @click="
            form.mode = item.value as LoginInput['mode'];
            error = '';
          "
        >
          {{ item.label }}
        </button>
      </div>
      <label v-if="form.mode === 'STUDENT'" class="field"
        >学校编号<input
          v-model.trim="form.school_id"
          required
          inputmode="numeric"
          pattern="[0-9]+"
          autocomplete="organization"
          placeholder="请输入老师提供的学校编号"
      /></label>
      <label class="field"
        >{{ usernameLabel
        }}<input
          v-model.trim="form.username"
          required
          :inputmode="form.mode === 'PHONE' ? 'tel' : 'text'"
          :maxlength="100"
          autocomplete="username"
          :placeholder="`请输入${usernameLabel}`"
      /></label>
      <label class="field"
        >密码
        <div class="password-field">
          <input
            v-model="form.password"
            required
            :type="visible ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="请输入登录密码"
            maxlength="100"
          /><button
            type="button"
            :aria-label="visible ? '隐藏密码' : '显示密码'"
            :aria-pressed="visible"
            @click="visible = !visible"
          >
            <AppIcon name="eye" :size="18" />
          </button></div
      ></label>
      <label v-if="captcha.captchaEnabled !== false" class="field"
        >验证码
        <div class="captcha-field">
          <input
            v-model.trim="form.code"
            required
            placeholder="请输入图形验证码"
            autocomplete="off"
            maxlength="12"
          /><button
            type="button"
            :disabled="captchaBusy"
            aria-label="刷新图形验证码"
            @click="refreshCaptcha"
          >
            <img
              v-if="captcha.img"
              :src="`data:image/gif;base64,${captcha.img}`"
              alt="图形验证码，点击刷新"
            /><span v-else>{{ captchaBusy ? "加载中…" : "重新加载" }}</span>
          </button>
        </div></label
      >
      <p v-if="error" role="alert" class="error-text">{{ error }}</p>
      <button
        type="submit"
        class="btn btn-primary submit-button"
        :disabled="busy || !ready"
      >
        <span v-if="busy" class="spinner" />{{
          busy ? "正在登录…" : "登录学习空间"
        }}<AppIcon v-if="!busy" name="arrow-right" :size="17" />
      </button>
      <p class="login-help">
        <AppIcon
          name="shield"
          :size="14"
        />账号由学校统一开通，忘记密码请联系老师。
      </p>
    </form>
  </div>
</template>
<style scoped lang="less">
.student-login-form {
  width: 100%;
  max-width: 370px;
}
.student-login-form > h2 {
  font-size: 29px;
  margin-bottom: 12px;
}
.student-login-form > h2 > span {
  color: #ffba57;
  font-size: 24px;
  margin-left: 12px;
}
.login-description {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 32px;
}
.segmented {
  margin-bottom: 26px;
}
.segmented button {
  flex: 1;
  font-size: 12px;
  padding: 10px 6px;
}
.field {
  margin-bottom: 20px;
  font-size: 12px;
}
.field input {
  min-height: 48px;
  font-size: 12px;
}
.password-field {
  position: relative;
}
.password-field input {
  padding-right: 48px;
}
.password-field button {
  position: absolute;
  right: 4px;
  top: 4px;
  bottom: 4px;
  width: 40px;
  border: 0;
  background: none;
  color: #a4adc1;
}
.captcha-field {
  display: flex;
  gap: 12px;
}
.captcha-field > input {
  flex: 1;
  width: 0;
}
.captcha-field > button {
  width: 116px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  background: #f6f8ff;
  border-radius: 10px;
  padding: 0;
  overflow: hidden;
  color: var(--primary);
  font-size: 12px;
}
.captcha-field img {
  width: 100%;
  height: 46px;
  object-fit: contain;
}
.submit-button {
  width: 100%;
  margin-top: 8px;
  min-height: 48px;
  justify-content: space-between;
  padding-left: 20px;
}
.submit-button:disabled {
  justify-content: center;
}
.login-help {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  font-size: 10px;
  line-height: 1.8;
  color: #a0aac0;
  margin: 22px 0 0;
}
.login-help svg {
  flex-shrink: 0;
  margin-top: 2px;
}
.preview-notice {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  font-size: 12px;
}
@media (max-width: 767px) {
  .student-login-form {
    max-width: 440px;
  }
  .student-login-form > h2 {
    font-size: 25px;
  }
  .login-description {
    margin-bottom: 25px;
  }
  .field {
    margin-bottom: 18px;
  }
  .segmented {
    margin-bottom: 22px;
  }
}
</style>
