<script setup lang="ts">
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
const auth = useAuthStore();
</script>
<template>
  <dl class="profile-details">
    <div>
      <dt>同学姓名</dt>
      <dd>{{ auth.user?.name || "—" }}</dd>
    </div>
    <div>
      <dt>所属学校</dt>
      <dd>{{ auth.user?.schoolName || auth.user?.schoolId || "—" }}</dd>
    </div>
    <div>
      <dt>所在班级</dt>
      <dd>{{ auth.user?.className || "暂未关联班级" }}</dd>
    </div>
    <div>
      <dt>学习账号</dt>
      <dd>{{ isPreview ? "设计预览账号" : auth.user?.id }}</dd>
    </div>
  </dl>
</template>
<style scoped>
.profile-details {
  margin: 0;
}
.profile-details > div {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 17px 0;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}
.profile-details > div:last-child {
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
