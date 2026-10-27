import { educationRequest, educationPublicRequest } from "@/shared/api/client";
import type { LoginInput, Captcha, StudentUser } from "./types";

export const getCaptcha = () =>
  educationPublicRequest<Captcha>({ url: "/captchaImage" });
export const signIn = (data: LoginInput) =>
  educationPublicRequest<{ token: string }>({
    url: "/biz/auth/login",
    method: "POST",
    data,
  });
export const signOut = () =>
  educationRequest({ url: "/logout", method: "POST" });
export const changePassword = (oldPassword: string, newPassword: string) =>
  educationRequest({
    url: "/biz/auth/password",
    method: "POST",
    data: { old_password: oldPassword, new_password: newPassword },
  });

export async function getCurrentUser(): Promise<StudentUser> {
  const me = await educationRequest<{
    user_id: string;
    name: string;
    school_id: string;
    must_change_pwd: boolean;
    phone_verified: boolean;
  }>({ url: "/biz/auth/me" });
  const user: StudentUser = {
    id: String(me.user_id),
    name: me.name,
    schoolId: String(me.school_id),
    mustChangePassword: me.must_change_pwd,
    phoneVerified: me.phone_verified,
  };
  // The force-password-change account may not yet access business options.
  if (!user.mustChangePassword) {
    const options = await educationRequest<{
      schools: { school_id: string; school_name: string }[];
      classes: { class_id: string; class_name: string; school_id: string }[];
    }>({ url: "/biz/options" });
    const school = options.schools.find(
      (item) => String(item.school_id) === user.schoolId,
    );
    user.schoolName = school?.school_name;
    // Students receive only their own class; never infer a class from an admin's wider list.
    if (options.classes.length === 1) {
      user.classId = String(options.classes[0]!.class_id);
      user.className = options.classes[0]!.class_name;
      user.schoolId = String(options.classes[0]!.school_id);
      user.schoolName = options.schools.find(
        (item) => String(item.school_id) === user.schoolId,
      )?.school_name;
    }
  }
  return user;
}
