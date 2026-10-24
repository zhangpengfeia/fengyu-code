import request from "@/utils/request";
export const learningGet = (path, params) =>
  request({ url: `/biz/learning/${path}`, params });
export const learningPost = (path, data) =>
  request({ url: `/biz/learning/${path}`, method: "post", data });
export const learningSave = (kind, id, data) =>
  request({
    url: `/biz/learning/dubbing/${kind}${id ? "/" + id : ""}`,
    method: id ? "put" : "post",
    data,
  });
export const learningDelete = (id) =>
  request({ url: `/biz/learning/dubbing/materials/${id}`, method: "delete" });
