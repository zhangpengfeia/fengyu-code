const educationKey = "fengyu.student.session";
let agentToken = "";
let agentExpiresAt = 0;

export const educationSession = {
  getToken: () => sessionStorage.getItem(educationKey) || "",
  setToken: (token: string) => sessionStorage.setItem(educationKey, token),
  clear: () => sessionStorage.removeItem(educationKey),
};

export const agentSession = {
  getToken: () => {
    if (Date.now() >= agentExpiresAt) return "";
    return agentToken;
  },
  setToken: (token: string, expiresIn = 900) => {
    agentToken = token;
    agentExpiresAt = Date.now() + Math.max(0, expiresIn - 15) * 1000;
  },
  clear: () => {
    agentToken = "";
    agentExpiresAt = 0;
  },
};

export interface AgentTokenResult {
  access_token: string;
  expires_in: number;
}

interface AgentTokenProvider {
  load: () => Promise<AgentTokenResult>;
  pending?: Promise<string>;
}
let tokenProvider: AgentTokenProvider | undefined;

/** A widget may only renew through its game session, never another saved login. */
export function registerAgentTokenProvider(
  load: () => Promise<AgentTokenResult>,
): () => void {
  const owner: AgentTokenProvider = { load };
  tokenProvider = owner;
  agentSession.clear();
  return () => {
    if (tokenProvider === owner) {
      tokenProvider = undefined;
      agentSession.clear();
    }
  };
}

export function getAgentTokenProvider(): (() => Promise<string>) | undefined {
  const owner = tokenProvider;
  if (!owner) return;
  return async () => {
    if (tokenProvider !== owner)
      throw new Error("学伴窗口已变化，请重新连接。");
    if (!owner.pending) {
      owner.pending = Promise.resolve()
        .then(() => {
          if (tokenProvider !== owner)
            throw new Error("学伴窗口已关闭，请重新打开。");
          return owner.load();
        })
        .then((result) => {
          if (tokenProvider !== owner)
            throw new Error("学伴窗口已关闭，请重新打开。");
          if (
            !result.access_token ||
            !Number.isFinite(result.expires_in) ||
            result.expires_in <= 15
          )
            throw new Error("学伴未返回有效会话，请重新连接。");
          agentSession.setToken(result.access_token, result.expires_in);
          return agentSession.getToken();
        });
    }
    const pending = owner.pending;
    try {
      return await pending;
    } finally {
      if (owner.pending === pending) owner.pending = undefined;
    }
  };
}
