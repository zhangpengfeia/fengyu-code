import router from "@/router";
import { me } from "@/api/education";
import cache from "@/plugins/cache";
import { ElMessageBox } from "element-plus";
import { login, logout, getInfo } from "@/api/login";
import { getToken, setToken, removeToken } from "@/utils/auth";
import { isHttp, isEmpty } from "@/utils/validate";
import useLockStore from "@/store/modules/lock";
import defAva from "@/assets/images/profile.jpg";

const useUserStore = defineStore("user", {
  state: () => ({
    token: getToken(),
    id: "",
    name: "",
    nickName: "",
    avatar: "",
    roles: [],
    mustChangePwd: false,
    permissions: [],
  }),
  actions: {
    // 登录
    login(userInfo) {
      const username = userInfo.username.trim();
      const password = userInfo.password;
      const code = userInfo.code;
      const uuid = userInfo.uuid;
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid, userInfo.mode, userInfo.school_id)
          .then((res) => {
            setToken(res.token);
            this.token = res.token;
            useLockStore().unlockScreen();
            resolve();
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
    // 获取用户信息
    getInfo() {
      return new Promise((resolve, reject) => {
        getInfo()
          .then((res) => {
            const user = res.user;
            let avatar = user.avatar || "";
            if (!isHttp(avatar)) {
              avatar = isEmpty(avatar)
                ? defAva
                : import.meta.env.VITE_APP_BASE_API + avatar;
            }
            if (res.roles && res.roles.length > 0) {
              // 验证返回的roles是否是一个非空数组
              this.roles = res.roles;
              this.permissions = res.permissions;
            } else {
              this.roles = ["ROLE_DEFAULT"];
            }
            this.id = user.userId;
            this.name = user.userName;
            this.nickName = user.nickName;
            this.avatar = avatar;
            cache.session.set("pwrChrtype", res.pwdChrtype);
            // Education first-login restriction is enforced by /biz/auth/me and the server.
            /* 过期密码提示 */
            if (!res.isDefaultModifyPwd && res.isPasswordExpired) {
              ElMessageBox.confirm(
                "您的密码已过期，请尽快修改密码！",
                "安全提示",
                {
                  confirmButtonText: "确定",
                  cancelButtonText: "取消",
                  type: "warning",
                },
              )
                .then(() => {
                  router.push({
                    name: "Profile",
                    params: { activeTab: "resetPwd" },
                  });
                })
                .catch(() => {});
            }
            me()
              .then((info) => {
                this.mustChangePwd = info.data.must_change_pwd;
                resolve(res);
              })
              .catch(reject);
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
    // 退出系统
    logOut() {
      return new Promise((resolve, reject) => {
        logout(this.token)
          .then(() => {
            this.token = "";
            this.roles = [];
            this.permissions = [];
            removeToken();
            resolve();
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
  },
});

export default useUserStore;
