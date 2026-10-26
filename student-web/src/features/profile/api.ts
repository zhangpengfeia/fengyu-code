import { educationRequest } from "@/shared/api/client";
export const redeemActivationCode = (code: string) =>
  educationRequest<{ id: string }>({
    url: "/biz/learning/activation/redeem",
    method: "POST",
    data: { code },
  });
