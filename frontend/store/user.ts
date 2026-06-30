import { defineStore } from "pinia";
import { ref } from "vue";
import * as authUtil from "@/utils/auth";
import { logger } from "@/utils/logger";
import { getApiBaseUrl, getServerBaseUrl } from "@/config";

export const useUserStore = defineStore("user", () => {
  const isLoggedIn = ref(authUtil.isLoggedIn());
  const userId = ref("");
  const phone = ref("");
  const nickname = ref("");
  const avatarUrl = ref("");

  async function init() {
    if (!authUtil.isLoggedIn()) return;
    logger.info("USER", "尝试自动恢复登录态");
    try {
      const profile = await authUtil.getMe();
      isLoggedIn.value = true;
      userId.value = profile.id;
      phone.value = profile.phone;
      nickname.value = profile.nickname;
      avatarUrl.value = profile.avatar_url;
      logger.info("USER", "自动恢复登录态成功", { userId: profile.id });
    } catch (e: any) {
      logger.warn("USER", "自动恢复登录态失败，清除 token", { msg: e.message });
      authUtil.logout();
      isLoggedIn.value = false;
    }
  }

  async function loginWithWechat() {
    logger.info("USER", "开始 wx.login");
    const loginRes = await new Promise<any>((resolve, reject) => {
      uni.login({
        success: resolve,
        fail: reject,
      });
    });
    const code = loginRes.code;
    logger.info("USER", "wx.login 成功，开始后端登录", { codeLen: code.length });
    const result = await authUtil.wxLogin(code);
    isLoggedIn.value = true;
    userId.value = result.user_id;
    phone.value = result.phone;
    nickname.value = result.nickname;
    logger.info("USER", "微信登录完成", { userId: result.user_id, isNew: result.is_new });
    return result;
  }

  async function loginWithPhone(code: string) {
    logger.info("USER", "开始 loginWithPhone");
    const result = await authUtil.phoneLogin(code);
    isLoggedIn.value = true;
    userId.value = result.user_id;
    phone.value = result.phone;
    nickname.value = result.nickname;
    logger.info("USER", "loginWithPhone 完成", { userId: result.user_id, isNew: result.is_new });
    return result;
  }

  async function uploadAvatar(filePath: string): Promise<string> {
    const token = uni.getStorageSync("access_token");
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: `${getApiBaseUrl()}/storage/upload`,
        filePath,
        name: "file",
        header: { Authorization: `Bearer ${token}` },
        success: (res: any) => {
          if (res.statusCode === 200) {
            const data = JSON.parse(res.data);
            resolve(data.url);
          } else {
            logger.error("USER", "头像上传失败", { status: res.statusCode, data: res.data });
            reject(new Error("上传失败(" + res.statusCode + ")"));
          }
        },
        fail: (err) => {
        logger.error("USER", "头像上传请求失败", { errMsg: err.errMsg });
        reject(new Error(err.errMsg || "上传失败"));
      },
      });
    });
  }

  async function updateProfile(data: { nickname?: string; avatar_url?: string }) {
    const token = uni.getStorageSync("access_token");
    return new Promise<void>((resolve, reject) => {
      uni.request({
        url: `${getApiBaseUrl()}/auth/profile`,
        method: "PUT",
        header: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        data,
        success: (res: any) => {
          if (res.statusCode === 200) {
            if (data.nickname) nickname.value = data.nickname;
            if (data.avatar_url) avatarUrl.value = data.avatar_url;
            resolve();
          } else {
            reject(new Error(res.data?.detail || "保存失败"));
          }
        },
        fail: (err) => reject(new Error(err.errMsg || "请求失败")),
      });
    });
  }

  async function doLogout() {
    authUtil.logout();
    isLoggedIn.value = false;
    userId.value = "";
    phone.value = "";
    nickname.value = "";
    avatarUrl.value = "";
  }

  return {
    isLoggedIn, userId, phone, nickname, avatarUrl,
    init, loginWithWechat, loginWithPhone, doLogout,
    uploadAvatar, updateProfile,
  };
});
