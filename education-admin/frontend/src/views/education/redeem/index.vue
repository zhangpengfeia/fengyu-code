<template>
  <div class="app-container">
    <el-card style="max-width: 600px"
      ><h2>激活学习权限</h2>
      <p>输入学具包中的激活码，兑换后从当天计算有效期。</p>
      <el-input
        v-model="code"
        maxlength="64"
        placeholder="请输入激活码"
        @keyup.enter="redeem" /><el-button
        type="primary"
        :loading="loading"
        style="margin-top: 20px"
        @click="redeem"
        >兑换</el-button
      ><el-alert
        v-if="success"
        title="兑换成功，可前往课件管理查看已开通内容。"
        type="success"
        :closable="false"
        style="margin-top: 20px"
    /></el-card>
  </div>
</template>
<script setup>
import { ref } from "vue";
import { learningPost } from "@/api/learning";
const code = ref(""),
  loading = ref(false),
  success = ref(false);
async function redeem() {
  loading.value = true;
  try {
    await learningPost("activation/redeem", { code: code.value.trim() });
    success.value = true;
    code.value = "";
  } finally {
    loading.value = false;
  }
}
</script>
