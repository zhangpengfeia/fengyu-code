import { ref, watch } from "vue";
import { useAuthStore } from "@/features/auth/store";
import { isPreview } from "@/shared/config";
import type { CompanionPreferences } from "./types";

const storagePrefix = "fengyu.companion.preferences";
const defaults: CompanionPreferences = {
  name: "枫同学",
  avatar: "",
  voice: "gentle",
};

function readPreferences(storageKey: string): CompanionPreferences {
  try {
    const saved = JSON.parse(
      localStorage.getItem(storageKey) || "{}",
    ) as Partial<CompanionPreferences>;
    return {
      name:
        typeof saved.name === "string" && saved.name.trim()
          ? saved.name.slice(0, 20)
          : defaults.name,
      avatar:
        typeof saved.avatar === "string" &&
        /^data:image\/(png|jpeg|webp);base64,/.test(saved.avatar)
          ? saved.avatar
          : "",
      voice:
        saved.voice && ["gentle", "energetic", "clear"].includes(saved.voice)
          ? saved.voice
          : "gentle",
    };
  } catch {
    return { ...defaults };
  }
}

const preferences = ref<CompanionPreferences>({ ...defaults });
let activeKey = "";

export function useCompanionPreferences() {
  const auth = useAuthStore();
  const userKey = () =>
    `${storagePrefix}.${isPreview ? "preview" : auth.user?.id || "guest"}`;
  watch(
    userKey,
    (key) => {
      if (activeKey !== key) {
        activeKey = key;
        preferences.value = readPreferences(key);
      }
    },
    { immediate: true },
  );
  function save(value: CompanionPreferences) {
    const normalized = {
      ...value,
      name: value.name.trim().slice(0, 20) || defaults.name,
    };
    // Write first: a full storage quota must not look like a successful save.
    localStorage.setItem(userKey(), JSON.stringify(normalized));
    preferences.value = normalized;
  }
  return { preferences, save };
}
