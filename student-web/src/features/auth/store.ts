import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { isPreview } from "@/shared/config";
import { educationSession, agentSession } from "@/shared/api/session";
import { getCurrentUser, signIn, signOut } from "./api";
import type { LoginInput, StudentUser } from "./types";

export const useAuthStore = defineStore("student-auth", () => {
  const user = ref<StudentUser | null>(null);
  const initialized = ref(false);
  const isAuthenticated = computed(
    () => isPreview || Boolean(educationSession.getToken() && user.value),
  );
  async function initialize() {
    if (initialized.value) return;
    if (isPreview) {
      user.value = {
        id: "preview-student",
        name: "杨同学",
        schoolId: "11000",
        schoolName: "未来实验小学",
        classId: "11001",
        className: "三年级（2）班",
        mustChangePassword: false,
        phoneVerified: true,
      };
    } else if (educationSession.getToken()) {
      user.value = await getCurrentUser();
    }
    initialized.value = true;
  }
  async function login(input: LoginInput) {
    agentSession.clear();
    const result = await signIn(input);
    if (!result.token) throw new Error("暂时无法完成登录，请稍后重试。");
    educationSession.setToken(result.token);
    try {
      user.value = await getCurrentUser();
      initialized.value = true;
    } catch (error) {
      educationSession.clear();
      user.value = null;
      throw error;
    }
  }
  function clear() {
    educationSession.clear();
    agentSession.clear();
    user.value = null;
    initialized.value = false;
  }
  async function logout() {
    try {
      if (!isPreview && educationSession.getToken()) await signOut();
      return true;
    } catch {
      return false;
    } finally {
      clear();
    }
  }
  return {
    user,
    initialized,
    isAuthenticated,
    initialize,
    login,
    logout,
    clear,
  };
});
