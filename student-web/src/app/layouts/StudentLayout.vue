<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import StudentNavigation from "./components/StudentNavigation.vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
const route = useRoute();
const auth = useAuthStore();
const dateLabel = new Intl.DateTimeFormat("zh-CN", {
  timeZone: "Asia/Shanghai",
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
const isCompanion = computed(() => route.path === "/companion");
</script>
<template>
  <div class="student-shell">
    <aside class="sidebar">
      <RouterLink to="/home" class="brand"
        ><span class="brand-mark"><AppIcon name="sparkles" :size="24" /></span
        ><span>枫语趣学<small>学有所趣 · 玩有所获</small></span></RouterLink
      >
      <p class="nav-caption">我的学习空间</p>
      <StudentNavigation />
      <RouterLink class="sidebar-companion" to="/companion"
        ><MascotAvatar :size="56" /><strong>有问题，问学伴</strong>
        <p>每一次好奇，都值得探索</p>
        <span>和我聊聊<AppIcon name="arrow-right" :size="15" /></span
      ></RouterLink>
      <div class="sidebar-bottom">
        <RouterLink to="/settings"
          ><AppIcon name="settings" :size="18" />账号设置</RouterLink
        ><small>让成长，看得见。</small>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="breadcrumb">
          <span class="desktop-crumb">学习空间<span>/</span></span
          ><strong>{{ route.meta.title }}</strong>
        </div>
        <div class="topbar-right">
          <span v-if="isPreview" class="preview-chip">设计预览</span
          ><span class="date-label">{{ dateLabel }}</span
          ><RouterLink
            to="/companion"
            class="icon-button assistant-shortcut"
            aria-label="打开智能学伴"
            ><AppIcon name="chat" :size="19" /></RouterLink
          ><RouterLink
            to="/profile"
            class="topbar-user"
            aria-label="查看我的空间"
            ><span class="student-avatar">{{
              auth.user?.name?.slice(-2, -1) || "学"
            }}</span
            ><span>{{ auth.user?.name || "同学" }}</span
            ><AppIcon name="chevron-down" :size="13"
          /></RouterLink>
        </div>
      </header>
      <main
        id="main-content"
        :class="['main-content', { 'companion-content': isCompanion }]"
      >
        <div v-if="isPreview" class="preview-banner">
          <span class="preview-dot" />当前为设计预览，学习数据与
          学伴回复均为示例。<RouterLink to="/login"
            >查看登录页<AppIcon name="arrow-right" :size="13"
          /></RouterLink>
        </div>
        <RouterView />
        <footer class="page-footer">
          每一点小进步，都在让你闪闪发光<span>枫语趣学 · 学生成长空间</span>
        </footer>
      </main>
    </div>
    <RouterLink
      v-if="!isCompanion"
      to="/companion"
      class="floating-companion"
      aria-label="和智能学伴聊聊"
      ><MascotAvatar :size="48" /><span>智能学伴</span><i
    /></RouterLink>
    <div class="bottom-nav"><StudentNavigation mobile /></div>
  </div>
</template>
<style scoped lang="less">
.student-shell {
  min-height: 100dvh;
}
.sidebar {
  width: var(--sidebar-width);
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 30;
  background: #fff;
  border-right: 1px solid var(--line);
  padding: 32px 21px 24px;
  display: flex;
  flex-direction: column;
}
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 23px;
  font-weight: 750;
  letter-spacing: -0.06em;
}
.brand-mark {
  width: 43px;
  height: 43px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--primary);
  border-radius: 14px;
  box-shadow: 0 5px 13px #305bff22;
  flex-shrink: 0;
}
.brand small {
  display: block;
  font-size: 9px;
  letter-spacing: 0.18em;
  color: #96a1bc;
  margin-top: 5px;
}
.nav-caption {
  margin: 44px 16px 17px;
  color: #a0aac0;
  font-size: 10px;
  letter-spacing: 0.1em;
}
.sidebar-companion {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(150deg, #edf5ff, #f3eeff);
  border: 1px solid #e5eaff;
  border-radius: 20px;
  margin-top: 42px;
  padding: 23px 8px 18px;
  overflow: hidden;
}
.sidebar-companion::after {
  content: "✧";
  position: absolute;
  right: 18px;
  top: 16px;
  color: #b5c7ff;
  font-size: 28px;
}
.sidebar-companion strong {
  font-size: 14px;
  margin-top: 13px;
}
.sidebar-companion p {
  font-size: 10px;
  color: #8b95b5;
  margin: 7px 0 15px;
}
.sidebar-companion > span:not(.mascot-avatar) {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 9px;
  padding: 9px 23px;
  color: var(--primary);
  font-size: 11px;
}
.sidebar-bottom {
  margin-top: auto;
  padding: 28px 14px 0;
}
.sidebar-bottom > a {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #7f8ca8;
}
.sidebar-bottom small {
  display: block;
  color: #b3bbce;
  font-size: 10px;
  margin-top: 22px;
}
.workspace {
  margin-left: var(--sidebar-width);
}
.topbar {
  height: 84px;
  background: #ffffffdc;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 36px;
  gap: 15px;
}
.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 12px;
}
.desktop-crumb {
  color: #9aa5bb;
}
.desktop-crumb > span {
  padding: 0 14px;
  color: #c9cfdc;
}
.breadcrumb strong {
  font-weight: 500;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 22px;
}
.date-label {
  color: #a0aac0;
  font-size: 11px;
}
.topbar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.student-avatar {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: #ffeed9;
  color: #b77c4d;
  font-size: 14px;
  font-weight: 600;
}
.preview-chip {
  font-size: 10px;
  border-radius: 6px;
  padding: 5px 8px;
  color: #9b7b3c;
  background: #fff6e4;
}
.main-content {
  max-width: 1430px;
  margin: 0 auto;
  padding: 30px 36px 20px;
}
.preview-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #8190ad;
  margin: -2px 0 24px;
  line-height: 1.7;
}
.preview-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #dfb064;
  flex-shrink: 0;
}
.preview-banner a {
  margin-left: auto;
  color: #8b99b6;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.page-footer {
  margin-top: 48px;
  color: #a9b3c9;
  font-size: 10px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.8;
}
.floating-companion {
  position: fixed;
  bottom: 26px;
  right: 28px;
  z-index: 35;
  border-radius: 40px;
  background: #fff;
  box-shadow: 0 6px 28px #305bff26;
  border: 1px solid #e1e9ff;
  padding: 4px 16px 4px 4px;
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}
.floating-companion i {
  position: absolute;
  width: 8px;
  height: 8px;
  background: #25c598;
  border: 2px solid #fff;
  border-radius: 50%;
  left: 39px;
  top: 4px;
}
.bottom-nav {
  display: none;
}
.assistant-shortcut {
  width: 34px;
  height: 34px;
  border-radius: 10px;
}
@media (min-width: 1600px) {
  .main-content {
    padding-top: 40px;
  }
  .topbar {
    padding: 0 48px;
  }
}
@media (max-width: 1100px) and (min-width: 768px) {
  :global(:root) {
    --sidebar-width: 200px;
  }
  .sidebar {
    padding: 28px 14px;
  }
  .brand {
    font-size: 21px;
  }
  .main-content {
    padding: 26px 24px;
  }
  .topbar {
    padding: 0 24px;
  }
  .date-label {
    display: none;
  }
}
@media (max-width: 767px) {
  .sidebar {
    display: none;
  }
  .workspace {
    margin-left: 0;
  }
  .topbar {
    height: 66px;
    padding: 0 20px;
  }
  .topbar-right {
    gap: 12px;
  }
  .desktop-crumb,
  .date-label,
  .topbar-user > span:not(.student-avatar),
  .topbar-user > svg {
    display: none;
  }
  .breadcrumb strong {
    font-size: 14px;
    font-weight: 600;
  }
  .main-content {
    padding: 22px 18px calc(96px + env(safe-area-inset-bottom));
  }
  .preview-banner {
    font-size: 10px;
    margin: 0 0 20px;
    gap: 6px;
  }
  .preview-banner a {
    display: none;
  }
  .bottom-nav {
    display: block;
    position: fixed;
    z-index: 40;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fffffff5;
    border-top: 1px solid var(--line);
    padding-bottom: env(safe-area-inset-bottom);
    backdrop-filter: blur(14px);
  }
  .floating-companion {
    bottom: calc(85px + env(safe-area-inset-bottom));
    right: 15px;
    padding: 3px;
    border-radius: 50%;
  }
  .floating-companion > span:not(.mascot-avatar) {
    display: none;
  }
  .page-footer {
    margin-top: 30px;
    font-size: 9px;
  }
  .page-footer > span {
    display: none;
  }
  .preview-chip {
    font-size: 9px;
    padding: 4px 6px;
  }
}
</style>
