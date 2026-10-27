import type { LiveWidgetSession } from "./api";

/** Only the game-session cookie is sent; browser-supplied identity is never used. */
export async function openLiveSession(
  signal: AbortSignal,
): Promise<LiveWidgetSession> {
  const controller = new AbortController();
  const cancel = () => controller.abort();
  signal.addEventListener("abort", cancel, { once: true });
  if (signal.aborted) cancel();
  let timedOut = false;
  const timeout = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, 30000);
  try {
    const response = await fetch("/play/planet/agent-session", {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "application/json" },
      signal: controller.signal,
    });
    const data = await response.json().catch(() => null);
    if (!response.ok || (data?.code !== undefined && data.code !== 200)) {
      const fallback =
        response.status === 401 || response.status === 403
          ? "游戏登录已过期或暂无学伴权限，请从教育平台重新进入星球。"
          : response.status === 503
            ? "智能学伴服务暂未配置完成，请稍后再试或联系老师。"
            : "暂时无法连接学伴服务，请稍后重试。";
      const message =
        typeof data?.msg === "string"
          ? data.msg
          : typeof data?.message === "string"
            ? data.message
            : fallback;
      throw new Error(message);
    }
    if (
      data?.mock === true ||
      (data?.demo !== undefined && typeof data.demo !== "boolean") ||
      typeof data?.access_token !== "string" ||
      data.access_token.length < 10 ||
      data.access_token.length > 8192 ||
      !Number.isInteger(data.expires_in) ||
      data.expires_in < 60 ||
      data.expires_in > 3600 ||
      !data.student ||
      ![data.student.id, data.student.name, data.student.school_id].every(
        (value) =>
          typeof value === "string" &&
          value.trim().length > 0 &&
          value.length <= 100,
      )
    )
      throw new Error("当前服务未返回有效的学伴会话，请稍后重新连接。");
    return {
      access_token: data.access_token,
      expires_in: data.expires_in,
      student: {
        id: data.student.id,
        name: data.student.name,
        school_id: data.student.school_id,
      },
      mock: false,
      demo: data.demo === true,
      evaluation_id: null,
    };
  } catch (cause) {
    if (signal.aborted) throw cause;
    if (timedOut) throw new Error("学伴连接超时，请稍后重试。");
    if (cause instanceof TypeError)
      throw new Error("暂时无法连接学伴服务，请稍后重试。");
    throw cause;
  } finally {
    clearTimeout(timeout);
    signal.removeEventListener("abort", cancel);
  }
}
