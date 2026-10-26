<script setup lang="ts">
import { ref } from "vue";
import AppIcon from "@/shared/components/AppIcon.vue";
import MascotAvatar from "@/shared/components/MascotAvatar.vue";
import { useCompanionPreferences } from "@/features/companion/preferences";
import ChatPanel from "./components/ChatPanel.vue";
import TutorPanel from "./components/TutorPanel.vue";
import CustomizePanel from "./components/CustomizePanel.vue";

const tab = ref<"chat" | "tutor" | "customize">("chat");
const tabs = [
  { id: "chat" as const, name: "和我聊聊", icon: "chat" },
  { id: "tutor" as const, name: "智能导师", icon: "sparkles" },
  { id: "customize" as const, name: "自定义学伴", icon: "settings" },
];
const { preferences } = useCompanionPreferences();
</script>

<template>
  <div class="page-stack companion-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">你的专属学习伙伴</p>
        <h1>有问题，找{{ preferences.name }}</h1>
        <p class="muted">陪你一起思考，让每一次探索都有收获。</p>
      </div>
    </div>
    <section class="companion-frame card">
      <header class="companion-bar">
        <img
          v-if="preferences.avatar"
          class="custom-avatar"
          :src="preferences.avatar"
          alt="自定义学伴头像"
        />
        <MascotAvatar v-else :size="48" />
        <div>
          <strong>{{ preferences.name }}<span> · 智能学伴</span></strong>
          <p><i />一起探索，一起成长</p>
        </div>
        <span class="companion-label"
          ><AppIcon name="sparkles" :size="14" />学习好伙伴</span
        >
      </header>
      <div class="companion-tabs" role="tablist" aria-label="智能学伴功能">
        <button
          v-for="item in tabs"
          :id="`companion-tab-${item.id}`"
          :key="item.id"
          type="button"
          role="tab"
          :aria-selected="tab === item.id"
          :aria-controls="`companion-panel-${item.id}`"
          :class="{ active: tab === item.id }"
          @click="tab = item.id"
        >
          <AppIcon :name="item.icon" :size="18" />{{ item.name }}
        </button>
      </div>
      <div
        v-show="tab === 'chat'"
        id="companion-panel-chat"
        role="tabpanel"
        aria-labelledby="companion-tab-chat"
      >
        <ChatPanel />
      </div>
      <div
        v-if="tab === 'tutor'"
        id="companion-panel-tutor"
        role="tabpanel"
        aria-labelledby="companion-tab-tutor"
      >
        <TutorPanel />
      </div>
      <div
        v-if="tab === 'customize'"
        id="companion-panel-customize"
        role="tabpanel"
        aria-labelledby="companion-tab-customize"
      >
        <CustomizePanel />
      </div>
    </section>
  </div>
</template>

<style scoped lang="less">
.companion-frame {
  padding: 0;
  overflow: hidden;
}
.companion-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 28px;
  background: linear-gradient(110deg, #e5eeff, #eff0ff 65%, #f7edff);
  border-bottom: 1px solid #e2e7fa;
  strong {
    color: #2647bf;
    font-size: 16px;
  }
  strong span {
    font-weight: 500;
  }
  p {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 7px 0 0;
    color: #6d7ca6;
    font-size: 12px;
  }
  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #4ebf9f;
  }
}
.custom-avatar {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 50%;
}
.companion-label {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  color: #6680bd;
  background: #ffffff91;
  border: 1px solid #fff;
  border-radius: 8px;
  font-size: 11px;
}
.companion-tabs {
  display: flex;
  padding: 0 28px;
  gap: 28px;
  border-bottom: 1px solid var(--line);
  background: white;
  button {
    display: flex;
    gap: 8px;
    align-items: center;
    position: relative;
    border: 0;
    background: transparent;
    padding: 18px 0;
    color: var(--muted);
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;
  }
  button.active {
    color: var(--primary);
    font-weight: 700;
    &::after {
      content: "";
      position: absolute;
      bottom: -1px;
      left: 0;
      right: 0;
      height: 3px;
      background: var(--primary);
      border-radius: 3px;
    }
  }
}
@media (max-width: 640px) {
  .companion-bar {
    padding: 18px 16px;
    strong {
      font-size: 14px;
    }
  }
  .companion-label {
    display: none;
  }
  .companion-tabs {
    gap: 0;
    justify-content: space-around;
    padding: 0 10px;
    button {
      font-size: 12px;
      gap: 6px;
    }
  }
}
</style>
