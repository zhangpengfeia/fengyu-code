// Preview mode is explicit, local and never selected as an API-error fallback.
export const isPreview = import.meta.env.MODE === "preview";
export const isMock = import.meta.env.MODE === "mock";
export const config = Object.freeze({
  educationBaseUrl: import.meta.env.VITE_EDUCATION_API_BASE || "/education-api",
  agentBaseUrl: import.meta.env.VITE_AGENT_API_BASE || "/agent-api",
  agentTokenEndpoint: isMock ? "/biz/auth/agent-token" : import.meta.env.VITE_AGENT_TOKEN_ENDPOINT || "",
});
