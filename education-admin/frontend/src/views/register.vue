<template>
  <div class="student-register">
    <el-card class="register-card"
      ><h2>学生注册</h2>
      <el-form label-width="100px" @submit.prevent="submit">
        <el-form-item label="学校" required
          ><el-cascader
            v-model="form.school_id"
            :options="schools"
            :props="{ emitPath: false }"
            filterable
            placeholder="选择省市区学校"
            @change="loadClasses"
        /></el-form-item>
        <el-form-item label="班级" required
          ><el-select v-model="form.class_id"
            ><el-option
              v-for="c in classes"
              :key="c.class_id"
              :value="String(c.class_id)"
              :label="c.class_name" /></el-select
        ></el-form-item>
        <el-form-item label="学号" required
          ><el-input
            v-model="form.student_no"
            maxlength="32"
            placeholder="保留学号前导零"
        /></el-form-item>
        <el-form-item label="姓名" required
          ><el-input v-model="form.nick_name" maxlength="30"
        /></el-form-item>
        <el-form-item label="手机号"
          ><el-input
            v-model="form.phone"
            maxlength="11"
            placeholder="选填，验证后可用于登录"
        /></el-form-item>
        <el-form-item label="密码" required
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="8至32位，包含至少两类字符"
        /></el-form-item>
        <el-form-item label="确认密码" required
          ><el-input
            v-model="confirm"
            type="password"
            show-password
            autocomplete="new-password"
        /></el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码" required
          ><el-input v-model="form.code" style="width: 140px" /><img
            :src="captchaImage"
            alt="点击刷新验证码"
            style="height: 38px; cursor: pointer; margin-left: 10px"
            @click="captcha"
        /></el-form-item>
        <el-form-item
          ><el-button type="primary" native-type="submit" :loading="loading"
            >注册</el-button
          ><router-link to="/login" style="margin-left: 20px"
            >返回登录</router-link
          ></el-form-item
        >
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";
import { getCodeImg } from "@/api/login";
const router = useRouter(),
  schools = ref([]),
  classes = ref([]),
  confirm = ref(""),
  loading = ref(false),
  captchaEnabled = ref(true),
  captchaImage = ref("");
const form = reactive({
  school_id: "",
  class_id: "",
  student_no: "",
  nick_name: "",
  phone: "",
  password: "",
  code: "",
  uuid: "",
});
const publicGet = (url, params) =>
  request({
    url: `/biz/student-registration/${url}`,
    params,
    headers: { isToken: false },
  });
async function captcha() {
  const r = await getCodeImg();
  captchaEnabled.value = r.captchaEnabled !== false;
  captchaImage.value = "data:image/gif;base64," + r.img;
  form.uuid = r.uuid;
  form.code = "";
}
async function loadClasses() {
  form.class_id = "";
  classes.value = (
    await publicGet("classes", { school_id: form.school_id })
  ).data;
}
async function submit() {
  if (form.password !== confirm.value) {
    ElMessage.error("两次输入的密码不一致");
    return;
  }
  if (
    !form.school_id ||
    !form.class_id ||
    !form.student_no ||
    !form.nick_name
  ) {
    ElMessage.error("请填写全部必填信息");
    return;
  }
  loading.value = true;
  try {
    const r = await request({
      url: "/biz/student-registration",
      method: "post",
      data: { ...form },
      headers: { isToken: false },
    });
    localStorage.setItem("education:lastSchool", form.school_id);
    await ElMessageBox.alert(
      r.data.audit_required
        ? "注册成功，请等待学校管理员审核。"
        : "注册成功，请使用学校和学号登录。",
      "学生注册",
    );
    router.push("/login");
  } finally {
    loading.value = false;
    await captcha();
  }
}
onMounted(async () => {
  schools.value = (await publicGet("tree")).data;
  await captcha();
});
</script>
<style scoped>
.student-register {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f4f7fb;
  padding: 30px;
}
.register-card {
  width: 560px;
  max-width: 100%;
}
</style>
