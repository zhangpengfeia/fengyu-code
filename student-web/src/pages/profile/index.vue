<script setup lang="ts">
import { computed, ref } from "vue";
import { useAuthStore } from "@/features/auth/store";
import ActionList from "@/shared/components/ActionList.vue";
import ActionListItem from "@/shared/components/ActionListItem.vue";
import ActionDialog from "@/shared/components/ActionDialog.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import ActivationRedeem from "./components/ActivationRedeem.vue";
import StudentProfileDetails from "./components/StudentProfileDetails.vue";
import ProfileAchievements from "./components/ProfileAchievements.vue";
const auth = useAuthStore();
const panel = ref<"details" | "achievements" | "activation" | null>(null);
const titles = {
  details: "学习档案",
  achievements: "我的荣誉",
  activation: "兑换课程激活码",
};
const panelVisible = computed({
  get: () => panel.value !== null,
  set: (value) => {
    if (!value) panel.value = null;
  },
});
</script>
<template>
  <div class="page-stack profile-page">
    <section class="profile-account" aria-label="当前学生">
      <MascotAvatar :size="58" />
      <div>
        <strong>{{ auth.user?.name || "同学" }}</strong>
        <p>
          {{ auth.user?.schoolName || "学校信息待完善"
          }}<span v-if="auth.user?.className">
            · {{ auth.user.className }}</span
          >
        </p>
      </div>
    </section>
    <ActionList label="我的功能">
      <ActionListItem label="学习档案" icon="user" @click="panel = 'details'" />
      <ActionListItem
        label="我的荣誉"
        icon="medal"
        tone="orange"
        @click="panel = 'achievements'"
      />
      <ActionListItem label="学习报告" icon="report" to="/reports" />
      <ActionListItem
        label="兑换课程激活码"
        icon="star"
        tone="orange"
        @click="panel = 'activation'"
      />
      <ActionListItem
        label="智能学伴"
        icon="chat"
        tone="green"
        to="/companion"
      />
      <ActionListItem
        label="账号设置"
        icon="settings"
        tone="purple"
        to="/settings"
      />
    </ActionList>
    <ActionDialog
      v-model:show="panelVisible"
      :title="panel ? titles[panel] : ''"
    >
      <StudentProfileDetails v-if="panel === 'details'" />
      <ProfileAchievements v-else-if="panel === 'achievements'" />
      <ActivationRedeem v-else-if="panel === 'activation'" />
    </ActionDialog>
  </div>
</template>
<style scoped>
.profile-account {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 18px;
}
.profile-account strong {
  font-size: 18px;
  font-weight: 600;
}
.profile-account p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}
@media (max-width: 767px) {
  .profile-account {
    padding: 20px 18px;
    border-radius: 16px;
  }
  .profile-account p {
    font-size: 11px;
  }
}
</style>
