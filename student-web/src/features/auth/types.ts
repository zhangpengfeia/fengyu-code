export interface StudentUser {
  id: string;
  name: string;
  schoolId: string;
  schoolName?: string;
  classId?: string;
  className?: string;
  mustChangePassword: boolean;
  phoneVerified?: boolean;
}
export interface LoginInput {
  mode: "ACCOUNT" | "STUDENT" | "PHONE";
  username: string;
  password: string;
  school_id: string;
  code: string;
  uuid: string;
}
export interface Captcha {
  captchaEnabled?: boolean;
  img?: string;
  uuid?: string;
}
