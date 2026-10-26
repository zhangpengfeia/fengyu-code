import { ref } from "vue";
const message = ref("");
let timer: ReturnType<typeof setTimeout> | undefined;
export function useToast() {
  function toast(value: string) {
    message.value = value;
    clearTimeout(timer);
    timer = setTimeout(() => {
      message.value = "";
    }, 3500);
  }
  return { message, toast };
}
