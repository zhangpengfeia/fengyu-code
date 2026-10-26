import axios, { type AxiosRequestConfig } from "axios";
import { config } from "@/shared/config";
import { educationSession, agentSession } from "./session";

export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function invalidateSession(token: string, agent: boolean) {
  if (agent) {
    if (agentSession.getToken() === token) agentSession.clear();
  } else if (token && educationSession.getToken() === token) {
    educationSession.clear();
    agentSession.clear();
    window.dispatchEvent(new Event("student:session-expired"));
  }
}

async function request<T>(
  baseURL: string,
  options: AxiosRequestConfig,
  token: string,
  agent = false,
): Promise<T> {
  try {
    const response = await axios.request({
      ...options,
      baseURL,
      timeout: options.timeout ?? 30000,
      headers: {
        ...options.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    const body = response.data;
    if (!agent && body && typeof body.code === "number" && body.code !== 200) {
      if (body.code === 401) {
        invalidateSession(token, agent);
      }
      throw new ApiError(body.msg || "请求失败，请稍后再试", body.code);
    }
    return !agent && body && Object.prototype.hasOwnProperty.call(body, "data")
      ? body.data
      : body;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      if (status === 401) {
        invalidateSession(token, agent);
      }
      const body = error.response?.data;
      const detail =
        typeof body?.detail === "string" ? body.detail : body?.detail?.message;
      const message =
        body?.msg || body?.message || body?.error?.message || detail;
      throw new ApiError(
        message ||
          (status === 403
            ? "当前账号暂无访问权限，请联系老师确认授权。"
            : status === 401
              ? "登录已过期，请重新登录。"
              : error.code === "ECONNABORTED"
                ? "服务响应超时，请重试。"
                : "暂时无法连接服务，请稍后重试。"),
        status,
      );
    }
    throw error;
  }
}

export const educationRequest = <T>(options: AxiosRequestConfig): Promise<T> =>
  request<T>(config.educationBaseUrl, options, educationSession.getToken());

let pendingToken: { owner: string; promise: Promise<string> } | undefined;
export async function ensureAgentToken(): Promise<string> {
  const token = agentSession.getToken();
  if (token) return token;
  if (!config.agentTokenEndpoint)
    throw new ApiError("智能学伴暂未开通，请稍后再试或联系老师。");
  const owner = educationSession.getToken();
  if (!owner) throw new ApiError("请先登录，再使用 智能学伴。", 401);
  if (pendingToken?.owner === owner) return pendingToken.promise;
  const promise = (async () => {
    const result = await educationRequest<{
      access_token: string;
      expires_in: number;
    }>({ url: config.agentTokenEndpoint, method: "POST" });
    if (educationSession.getToken() !== owner)
      throw new ApiError("登录状态已变化，请重新打开学伴。", 401);
    if (!result.access_token)
      throw new ApiError("智能学伴暂时无法登录，请稍后重试。");
    agentSession.setToken(result.access_token, result.expires_in);
    return agentSession.getToken();
  })();
  pendingToken = { owner, promise };
  try {
    return await promise;
  } finally {
    if (pendingToken?.promise === promise) pendingToken = undefined;
  }
}

export async function agentRequest<T>(options: AxiosRequestConfig): Promise<T> {
  return request<T>(
    config.agentBaseUrl,
    options,
    await ensureAgentToken(),
    true,
  );
}
