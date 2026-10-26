<script setup lang="ts">
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
const auth = useAuthStore();
</script>
<template>
  <dl class="account-details">
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
</template>
<style scoped>
.account-details {
  margin: 0;
}
.account-details > div {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 17px 0;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}
.account-details > div:last-child {
  border: 0;
}
dt {
  color: var(--muted);
  flex-shrink: 0;
}
dd {
  margin: 0;
  text-align: right;
  overflow-wrap: anywhere;
}
</style>
