<template>
  <main class="education-login">
    <div class="login-story">
      <div class="login-brand">
        <span class="login-symbol">◈</span> 智慧教育平台
      </div>
      <div class="story-copy">
        <span>CONNECTED LEARNING</span>
        <h1>让教学资源<br />连接每一次成长</h1>
        <p>课件、互动游戏与学校管理，<br />在同一个工作台有序展开。</p>
        <div class="story-orbit">
          <i>◈</i><b>π</b><em>✦</em><small>01 · 02 · 03</small>
        </div>
      </div>
      <footer>安全访问 · 按需授权 · 有序教学</footer>
    </div>
    <div class="login-workspace">
      <div class="login-form-wrap">
        <div class="login-small-brand">智慧教育 · 管理工作台</div>
        <h2>欢迎登录</h2>
        <p class="login-description">使用你的账号进入教学空间</p>
        <el-form
          ref="formRef"
          :model="form"
          label-position="top"
          @submit.prevent="submit"
          ><el-form-item label="登录方式"
            ><el-segmented
              v-model="form.mode"
              :options="modes"
              block /></el-form-item
          ><el-form-item v-if="form.mode === 'STUDENT'" label="学校ID"
            ><el-input
              v-model="form.school_id"
              :disabled="lockedSchool"
              placeholder="请输入学校ID，或使用学校专属登录链接"
            />
            <div v-if="schoolName" class="edu-field-hint">
              {{ schoolName }}
            </div></el-form-item
          ><el-form-item
            :label="
              form.mode === 'PHONE'
                ? '已验证手机号'
                : form.mode === 'STUDENT'
                  ? '学生学号'
                  : '登录账号'
            "
            ><el-input
              v-model="form.username"
              size="large"
              :placeholder="
                form.mode === 'STUDENT'
                  ? '请输入学号，保留前导零'
                  : form.mode === 'PHONE'
                    ? '请输入已验证的手机号'
                    : '请输入用户名'
              "
              autocomplete="username"
              maxlength="32"
              prefix-icon="User" /></el-form-item
          ><el-form-item label="密码"
            ><el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入密码"
              prefix-icon="Lock" /></el-form-item
          ><el-form-item v-if="captchaEnabled" label="验证码"
            ><div class="login-captcha">
              <el-input
                v-model="form.code"
                size="large"
                placeholder="请输入计算结果"
                maxlength="8"
              /><button type="button" title="刷新验证码" @click="captcha">
                <img :src="captchaImage" alt="登录验证码，点击刷新" />
              </button></div></el-form-item
          ><el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%; margin-top: 8px"
            native-type="submit"
            >登录工作台</el-button
          ></el-form
        >
        <p class="login-help">忘记密码或尚未开通账号？请联系学校管理员。</p>
        <div class="login-note">
          首次使用管理员交付的初始密码登录后，请完成密码修改。
        </div>
      </div>
      <footer>智慧教育平台 © 2026</footer>
    </div>
  </main>
</template>
<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getCodeImg } from "@/api/login";
import { schoolEntry } from "@/api/education";
import useUserStore from "@/store/modules/user";
const route = useRoute(),
  router = useRouter(),
  store = useUserStore(),
  loading = ref(false),
  formRef = ref(),
  captchaImage = ref(""),
  captchaEnabled = ref(true),
  lockedSchool = ref(false),
  schoolName = ref("");
const modes = [
  { label: "账号", value: "ACCOUNT" },
  { label: "手机号", value: "PHONE" },
  { label: "学校学号", value: "STUDENT" },
];
const form = reactive({
  mode: "ACCOUNT",
  username: "",
  password: "",
  school_id: localStorage.getItem("education:lastSchool") || "",
  code: "",
  uuid: "",
});
async function captcha() {
  try {
    const r = await getCodeImg();
    captchaEnabled.value = r.captchaEnabled !== false;
    captchaImage.value = "data:image/gif;base64," + r.img;
    form.uuid = r.uuid;
    form.code = "";
  } catch (e) {
    ElMessage.error("暂时无法获取验证码，请检查后端服务");
  }
}
async function submit() {
  if (
    !form.username ||
    !form.password ||
    (form.mode === "STUDENT" && !form.school_id)
  ) {
    ElMessage.warning("请完整填写登录信息");
    return;
  }
  loading.value = true;
  try {
    await store.login(form);
    if (form.mode === "STUDENT")
      localStorage.setItem("education:lastSchool", form.school_id);
    const target = String(route.query.redirect || "/index");
    await router.push(
      target.startsWith("/") && !target.startsWith("//") ? target : "/index",
    );
  } catch (e) {
    form.password = "";
    await captcha();
  } finally {
    loading.value = false;
  }
}
onMounted(async () => {
  await captcha();
  if (route.params.schoolCode) {
    const r = await schoolEntry(route.params.schoolCode);
    form.mode = "STUDENT";
    form.school_id = String(r.data.school_id);
    schoolName.value = r.data.school_name;
    lockedSchool.value = true;
  }
});
</script>
<style scoped>
.education-login {
  display: flex;
  min-height: 100vh;
  background: #fff;
  color: #243752;
}
.login-story {
  width: 48%;
  min-height: 100vh;
  background: #152e56;
  color: white;
  padding: 48px 7%;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
.login-brand {
  font-weight: 600;
  letter-spacing: 2px;
  font-size: 21px;
  white-space: nowrap;
}
.login-symbol {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #3979e7;
  border-radius: 12px;
  width: 38px;
  height: 38px;
  margin-right: 8px;
}
.story-copy {
  margin: auto 0;
  position: relative;
}
.story-copy > span {
  font-size: 11px;
  letter-spacing: 3px;
  color: #9db7df;
}
.story-copy h1 {
  font-size: 44px;
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 2px;
  margin: 22px 0;
}
.story-copy p {
  font-size: 16px;
  color: #aec2df;
  line-height: 2;
}
.story-orbit {
  position: relative;
  height: 180px;
  width: 260px;
  margin-top: 40px;
  border: 1px solid #5478aa66;
  border-radius: 50%;
  transform: rotate(-18deg);
}
.story-orbit:before {
  content: "";
  position: absolute;
  inset: 24px;
  border: 1px solid #5478aa66;
  border-radius: 50%;
}
.story-orbit i {
  font-style: normal;
  font-size: 70px;
  position: absolute;
  left: 90px;
  top: 30px;
  color: #81c7fe;
}
.story-orbit b {
  position: absolute;
  right: 0;
  top: 40px;
  color: #f4c984;
  font-size: 30px;
}
.story-orbit em {
  position: absolute;
  bottom: 10px;
  left: 5px;
  color: #c8acff;
  font-size: 38px;
}
.story-orbit small {
  position: absolute;
  bottom: 20px;
  right: 45px;
  color: #92add2;
  letter-spacing: 4px;
  font-size: 11px;
}
.login-story footer {
  font-size: 12px;
  color: #98b3d7;
  letter-spacing: 2px;
}
.login-workspace {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 60px 30px 25px;
  position: relative;
}
.login-form-wrap {
  width: 100%;
  max-width: 385px;
  margin: auto;
}
.login-small-brand {
  font-size: 13px;
  color: #7d8ba0;
  letter-spacing: 1px;
}
.login-workspace h2 {
  font-size: 30px;
  letter-spacing: 1px;
  margin: 22px 0 12px;
}
.login-description {
  color: #8b97a7;
  margin-bottom: 32px;
}
.login-help {
  font-size: 12px;
  text-align: center;
  color: #8793a4;
  margin-top: 24px;
}
.login-note {
  font-size: 12px;
  color: #8c98a9;
  background: #f6f8fb;
  border-radius: 6px;
  padding: 14px;
  line-height: 1.8;
  margin-top: 32px;
}
.login-workspace footer {
  font-size: 11px;
  color: #a4afbf;
  padding-top: 40px;
}
.login-captcha {
  display: flex;
  width: 100%;
  gap: 12px;
}
.login-captcha button {
  border: 1px solid #dce3ef;
  padding: 0;
  background: #f3f6fb;
  cursor: pointer;
  width: 120px;
  border-radius: 4px;
}
.login-captcha img {
  width: 100%;
  height: 40px;
  object-fit: contain;
  display: block;
}
@media (max-width: 900px) {
  .login-story {
    width: 42%;
    padding: 40px 5%;
  }
  .story-copy h1 {
    font-size: 32px;
  }
}
@media (max-width: 640px) {
  .login-story {
    display: none;
  }
  .login-workspace {
    padding: 48px 24px;
    min-height: 100vh;
  }
}
</style>
