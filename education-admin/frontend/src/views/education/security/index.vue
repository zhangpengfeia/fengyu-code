<template>
  <div class="app-container education-page">
    <div class="edu-page-heading">
      <div>
        <span class="edu-eyebrow">我的账号</span>
        <h1>账号安全</h1>
        <p>修改密码与验证手机号。初始密码修改后才能进入其他功能。</p>
      </div>
    </div>
    <el-row :gutter="24"
      ><el-col :xs="24" :md="12"
        ><el-card shadow="never" class="edu-card"
          ><template #header>修改登录密码</template
          ><el-alert
            v-if="store.mustChangePwd"
            title="当前使用初始密码，请先完成修改。"
            type="warning"
            :closable="false"
          /><el-form
            label-position="top"
            class="edu-gap"
            @submit.prevent="submit"
            ><el-form-item label="原密码"
              ><el-input
                v-model="pwd.old_password"
                type="password"
                show-password
                autocomplete="current-password" /></el-form-item
            ><el-form-item label="新密码"
              ><el-input
                v-model="pwd.new_password"
                type="password"
                show-password
                autocomplete="new-password"
              />
              <div class="edu-field-hint">
                8至32位，包含字母、数字、符号中的至少两类
              </div></el-form-item
            ><el-form-item label="确认新密码"
              ><el-input
                v-model="confirm"
                type="password"
                show-password
                autocomplete="new-password" /></el-form-item
            ><el-button type="primary" :loading="saving" @click="submit"
              >保存并重新登录</el-button
            ></el-form
          ></el-card
        ></el-col
      ><el-col :xs="24" :md="12"
        ><el-card v-if="!store.mustChangePwd" shadow="never" class="edu-card"
          ><template #header>手机号验证</template
          ><el-form label-position="top"
            ><el-form-item label="手机号"
              ><el-input v-model="phone.phone" maxlength="11" /></el-form-item
            ><el-form-item label="验证码"
              ><el-input v-model="phone.code" maxlength="6"
                ><template #append
                  ><el-button :loading="sending" @click="send"
                    >发送验证码</el-button
                  ></template
                ></el-input
              ></el-form-item
            ><el-alert
              v-if="developmentCode"
              :title="`开发环境模拟验证码：${developmentCode}`"
              type="info"
              :closable="false"
            /><el-form-item label="当前登录密码"
              ><el-input
                v-model="phone.password"
                type="password"
                show-password /></el-form-item
            ><el-button type="primary" :loading="binding" @click="bind"
              >验证并绑定</el-button
            ></el-form
          ></el-card
        ></el-col
      ></el-row
    >
  </div>
</template>
<script setup>
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { changePassword, sendPhoneCode, bindPhone } from "@/api/education";
import useUserStore from "@/store/modules/user";
import { removeToken } from "@/utils/auth";
const store = useUserStore(),
  pwd = reactive({ old_password: "", new_password: "" }),
  confirm = ref(""),
  saving = ref(false),
  sending = ref(false),
  binding = ref(false),
  developmentCode = ref(""),
  phone = reactive({ phone: "", code: "", password: "" });
async function submit() {
  if (
    !pwd.old_password ||
    pwd.new_password !== confirm.value ||
    pwd.new_password.length < 8
  ) {
    ElMessage.warning("请检查原密码、新密码长度及两次输入是否一致");
    return;
  }
  saving.value = true;
  try {
    await changePassword(pwd);
    removeToken();
    store.token = "";
    store.roles = [];
    ElMessage.success("密码已更新，请重新登录");
    location.href = "/login";
  } finally {
    saving.value = false;
  }
}
async function send() {
  sending.value = true;
  try {
    const r = await sendPhoneCode(phone.phone);
    developmentCode.value = r.data.development_code || "";
    ElMessage.success("验证码已发送");
  } finally {
    sending.value = false;
  }
}
async function bind() {
  binding.value = true;
  try {
    await bindPhone(phone);
    phone.code = "";
    phone.password = "";
    developmentCode.value = "";
    ElMessage.success("验证成功，可以使用该手机号登录");
  } finally {
    binding.value = false;
  }
}
</script>
