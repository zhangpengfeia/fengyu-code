<script setup lang="ts">
import { ref } from "vue";
import { Popup } from "vant";
import "vant/es/popup/style";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import AppIcon from "@/shared/components/AppIcon.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import AchievementBadge from "./components/AchievementBadge.vue";
import ActivationRedeem from "./components/ActivationRedeem.vue";
const auth = useAuthStore();
const redeemOpen = ref(false);
const achievements = [
  { name: "初次探索", color: "#ff9d42", icon: "star" },
  { name: "思维新星", color: "#5676ff", icon: "sparkles" },
  { name: "坚持不懈", color: "#9162ef", icon: "medal" },
  { name: "学习达人", color: "#ef729d", icon: "trophy" },
  { name: "奇思妙想", color: "#31bfaa", icon: "target" },
  { name: "勇于挑战", color: "#f3b745", icon: "shield" },
];
</script>
<template>
  <div class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">记录每一次闪光</p>
        <h1>我的成长空间</h1>
        <p>收集每一次收获，记录每一步成长。</p>
      </div>
      <RouterLink class="icon-button" to="/settings" aria-label="账号设置"
        ><AppIcon name="settings"
      /></RouterLink>
    </div>
    <section class="profile-hero">
      <div class="profile-identity">
        <MascotAvatar :size="80" />
        <div>
          <span class="profile-label">小小探索家</span>
          <h2>{{ auth.user?.name }}</h2>
          <p>
            {{ auth.user?.schoolName || "学校信息待完善"
            }}<span v-if="auth.user?.className">
              · {{ auth.user.className }}</span
            >
          </p>
        </div>
      </div>
      <div class="profile-hero-star">
        <AppIcon name="medal" :size="60" /><span>每一步都算数</span>
      </div>
    </section>
    <div class="profile-grid">
      <div class="page-stack">
        <section class="card">
          <div class="section-heading">
            <h2>我的荣誉墙</h2>
            <span v-if="isPreview" class="badge">示例徽章</span>
          </div>
          <div v-if="isPreview" class="achievements">
            <AchievementBadge
              v-for="item in achievements"
              :key="item.name"
              v-bind="item"
            />
          </div>
          <div v-else class="honor-empty">
            <AppIcon name="medal" :size="36" />
            <p>完成测评后，可在学习报告中查看本局徽章。</p>
            <RouterLink class="btn btn-soft" to="/reports"
              >查看测评报告</RouterLink
            >
          </div>
        </section>
        <section class="card">
          <div class="section-heading">
            <h2>学习档案</h2>
            <AppIcon name="book" :size="20" class="muted" />
          </div>
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
        </section>
      </div>
      <aside class="page-stack">
        <section class="card quick-menu">
          <RouterLink to="/reports"
            ><span class="menu-icon blue"><AppIcon name="report" /></span>
            <div>
              <strong>我的学习报告</strong><small>发现自己的成长与进步</small>
            </div>
            <AppIcon name="chevron-right" :size="16" /></RouterLink
          ><button @click="redeemOpen = true">
            <span class="menu-icon orange"><AppIcon name="star" /></span>
            <div>
              <strong>兑换课程激活码</strong
              ><small>解锁更多有趣的学习内容</small>
            </div>
            <AppIcon name="chevron-right" :size="16" /></button
          ><RouterLink to="/settings"
            ><span class="menu-icon purple"><AppIcon name="shield" /></span>
            <div>
              <strong>账号与安全</strong><small>管理密码和登录状态</small>
            </div>
            <AppIcon name="chevron-right" :size="16"
          /></RouterLink>
        </section>
        <RouterLink class="companion-promo" to="/companion"
          ><MascotAvatar :size="54" />
          <h3>成长路上，有我陪你</h3>
          <p>不懂就问，好奇心永不打烊。</p>
          <span>去和学伴聊聊<AppIcon name="arrow-right" :size="15" /></span
        ></RouterLink>
      </aside>
    </div>
    <Popup
      v-model:show="redeemOpen"
      round
      closeable
      position="bottom"
      :style="{
        maxWidth: '520px',
        left: '50%',
        transform: 'translateX(-50%)',
        padding: '30px 24px calc(26px + env(safe-area-inset-bottom))',
      }"
      ><h2>兑换课程激活码</h2>
      <ActivationRedeem
    /></Popup>
  </div>
</template>
<style scoped lang="less">
.profile-hero {
  background: linear-gradient(115deg, #315dff, #6f83ff);
  padding: 32px;
  border-radius: 24px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
}
.profile-hero::after {
  content: "";
  width: 280px;
  height: 280px;
  border: 45px solid #ffffff0a;
  border-radius: 50%;
  position: absolute;
  right: -40px;
  top: -115px;
  pointer-events: none;
}
.profile-identity {
  display: flex;
  align-items: center;
  gap: 22px;
}
.profile-label {
  font-size: 10px;
  letter-spacing: 0.08em;
  color: #d0d9ff;
}
.profile-identity h2 {
  font-size: 28px;
  margin: 7px 0 9px;
}
.profile-identity p {
  font-size: 11px;
  color: #d6dfff;
  margin: 0;
  line-height: 1.8;
}
.profile-hero-star {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #ffe395;
  margin-right: 15px;
}
.profile-hero-star > span {
  font-size: 10px;
  color: #e4eaff;
}
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 24px;
}
.achievements {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  padding: 15px 0 9px;
  flex-wrap: wrap;
}
.profile-details {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.profile-details > div {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  padding: 16px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}
.profile-details > div:last-child {
  border: 0;
  padding-bottom: 0;
}
dt {
  color: var(--muted);
}
dd {
  margin: 0;
  text-align: right;
}
.quick-menu {
  padding: 6px 22px;
}
.quick-menu > a,
.quick-menu > button {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 20px 0;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: none;
  width: 100%;
  text-align: left;
}
.quick-menu > :last-child {
  border: 0;
}
.quick-menu div {
  flex: 1;
}
.quick-menu strong {
  display: block;
  font-size: 12px;
  font-weight: 600;
}
.quick-menu small {
  display: block;
  color: var(--muted);
  font-size: 10px;
  margin-top: 6px;
}
.quick-menu > * > svg {
  color: #b0bbd2;
}
.menu-icon {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  display: grid;
  place-items: center;
}
.blue {
  background: #eef2ff;
  color: #5b79ee;
}
.orange {
  background: #fff5e6;
  color: #e7a552;
}
.purple {
  background: #f3eeff;
  color: #9a74e8;
}
.companion-promo {
  padding: 26px;
  background: linear-gradient(135deg, #eaf4ff, #f5f0ff);
  border-radius: 22px;
  text-align: center;
  border: 1px solid #e4eaff;
}
.companion-promo h3 {
  margin: 14px 0 10px;
  font-size: 17px;
}
.companion-promo p {
  font-size: 11px;
  color: #8b96b3;
}
.companion-promo > span:not(.mascot-avatar) {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  color: var(--primary);
  margin-top: 7px;
}
.honor-empty {
  text-align: center;
  padding: 15px;
  color: #a3b0cd;
}
.honor-empty p {
  font-size: 12px;
  line-height: 1.8;
  margin-top: 14px;
}
@media (max-width: 767px) {
  .profile-hero {
    padding: 24px 20px;
    border-radius: 20px;
  }
  .profile-identity {
    gap: 15px;
  }
  .profile-identity h2 {
    font-size: 24px;
  }
  .profile-identity :deep(.mascot-avatar) {
    width: 62px !important;
    height: 62px !important;
  }
  .profile-hero-star {
    margin: 0;
  }
  .profile-hero-star svg {
    width: 42px;
  }
  .profile-hero-star > span {
    font-size: 8px;
  }
  .profile-identity p {
    font-size: 10px;
  }
  .profile-grid {
    grid-template-columns: 1fr;
    gap: 22px;
  }
  .achievements {
    gap: 20px;
    justify-content: space-around;
  }
  .profile-grid > .page-stack {
    gap: 22px;
  }
}
</style>
