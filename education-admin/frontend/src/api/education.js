import request from "@/utils/request";
export const listCatalog = (kind, params) =>
  request({ url: `/biz/catalog/${kind}`, params });
export const getCatalog = (kind, id, edit = false) =>
  request({ url: `/biz/catalog/${kind}/${id}`, params: { edit } });
export const saveCatalog = (kind, id, data) =>
  request({
    url: `/biz/catalog/${kind}${id ? "/" + id : ""}`,
    method: id ? "put" : "post",
    data,
  });
export const deleteCatalog = (kind, id) =>
  request({ url: `/biz/catalog/${kind}/${id}`, method: "delete" });
export const launchGame = (id) =>
  request({
    url: `/biz/games/${id}/launch`,
    method: "post",
    headers: { repeatSubmit: false },
  });
export const options = () => request({ url: "/biz/options" });
export const me = () => request({ url: "/biz/auth/me" });
export const listUsers = (params) => request({ url: "/biz/users", params });
export const getUser = (id) => request({ url: `/biz/users/${id}` });
export const saveUser = (id, data) =>
  request({
    url: `/biz/users${id ? "/" + id : ""}`,
    method: id ? "put" : "post",
    data,
  });
export const resetPassword = (id) =>
  request({ url: `/biz/users/${id}/reset-password`, method: "post" });
export const versions = (id) =>
  request({ url: `/biz/courseware/${id}/versions` });
export const listGrants = (student_user_id) =>
  request({ url: "/biz/entitlements", params: { student_user_id } });
export const grant = (data) =>
  request({ url: "/biz/entitlements", method: "post", data });
export const revoke = (id, reason) =>
  request({
    url: `/biz/entitlements/${id}/revoke`,
    method: "post",
    data: { reason },
  });
export const changePassword = (data) =>
  request({ url: "/biz/auth/password", method: "post", data });
export const sendPhoneCode = (phone) =>
  request({ url: "/biz/auth/phone/code", method: "post", data: { phone } });
export const bindPhone = (data) =>
  request({ url: "/biz/auth/phone/bind", method: "post", data });
export const schoolEntry = (code) =>
  request({
    url: `/biz/auth/school/${encodeURIComponent(code)}`,
    headers: { isToken: false },
  });
export const subjects = {
  MATH: "数学",
  READING: "阅读",
  SCIENCE: "科创",
  CREATIVE: "中创",
};
export const labels = {
  PUBLISHED: "已发布",
  DRAFT: "草稿",
  DISABLED: "已停用",
  ACTIVE: "正常",
  ARCHIVED: "已归档",
  REVOKED: "已撤销",
  INTERNAL: "内部游戏",
  EXTERNAL: "外部引用",
  TEACHER_ONLY: "仅老师",
  COURSEWARE: "跟随课件授权",
  STUDENT: "独立学生授权",
  edu_teacher: "老师",
  edu_student: "学生",
  edu_school: "学校管理员",
  admin: "超级管理员",
  edu_region: "区域管理员",
};
export const statusType = (s) =>
  ["PUBLISHED", "ACTIVE", "0"].includes(String(s))
    ? "success"
    : ["DISABLED", "REVOKED", "1"].includes(String(s))
      ? "danger"
      : "info";
