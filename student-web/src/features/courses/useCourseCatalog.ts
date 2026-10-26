import { onMounted, ref } from "vue";
import { listCourses } from "./api";
import type { Course } from "./types";

export function useCourseCatalog() {
  const courses = ref<Course[]>([]);
  const loading = ref(true);
  const error = ref("");
  let request = 0;

  async function load() {
    const current = ++request;
    loading.value = true;
    error.value = "";
    try {
      const result = await listCourses();
      if (current === request) courses.value = result;
    } catch (cause) {
      if (current === request)
        error.value =
          cause instanceof Error ? cause.message : "课程加载失败，请重试。";
    } finally {
      if (current === request) loading.value = false;
    }
  }

  onMounted(load);
  return { courses, loading, error, load };
}
