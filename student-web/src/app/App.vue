<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/features/auth/store";
import { useToast } from "@/shared/composables/useToast";
const router = useRouter();
const auth = useAuthStore();
const { message, toast } = useToast();
function expired() {
  auth.clear();
  if (router.currentRoute.value.name !== "login") {
    toast("登录已过期，请重新登录。");
    void router.replace({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath },
    });
  }
}
onMounted(() => window.addEventListener("student:session-expired", expired));
onUnmounted(() =>
  window.removeEventListener("student:session-expired", expired),
);
</script>
<template>
  <RouterView /><Transition name="toast"
    ><div v-if="message" class="feedback-toast" role="status">
      {{ message }}
    </div></Transition
  >
</template>
<style scoped>
.feedback-toast {
  position: fixed;
  bottom: 110px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background: #1c2650ed;
  color: #fff;
  padding: 13px 22px;
  border-radius: 12px;
  box-shadow: 0 8px 32px #1c265022;
  font-size: 13px;
  max-width: calc(100vw - 36px);
  text-align: center;
  line-height: 1.6;
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}
</style>
