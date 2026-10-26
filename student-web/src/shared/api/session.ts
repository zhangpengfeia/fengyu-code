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
