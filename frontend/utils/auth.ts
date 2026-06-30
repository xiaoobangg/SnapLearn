import { getApiBaseUrl } from "@/config";
import { logger } from "@/utils/logger";
const getUrl = (path: string) => `${getApiBaseUrl()}${path}`;

interface AuthResult {
  user_id: string;
  phone: string;
  nickname: string;
  is_new: boolean;
  token: string;
}

export interface UserProfile {
  id: string;
  phone: string;
  nickname: string;
  avatar_url: string;
  created_at: string;
}

/** 微信登录：wx.login() code → openid（个人号可用） */
export async function wxLogin(code: string): Promise<AuthResult> {
  const url = getUrl("/auth/wechat-login");
  logger.info("AUTH", "请求微信登录", { codeLen: code.length });
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method: "POST",
      header: { "Content-Type": "application/json" },
      data: { code },
      success: (res: any) => {
        if (res.statusCode === 200) {
          logger.info("AUTH", "微信登录成功", { userId: res.data?.user_id, isNew: res.data?.is_new });
          uni.setStorageSync("access_token", res.data.token);
          resolve(res.data as AuthResult);
        } else {
          logger.warn("AUTH", "微信登录接口返回异常", { status: res.statusCode, detail: res.data?.detail });
          reject(new Error(res.data?.detail || "登录失败"));
        }
      },
      fail: (err) => {
        logger.error("AUTH", "微信登录请求失败", { errMsg: err.errMsg });
        reject(new Error(err.errMsg || "网络请求失败"));
      },
    });
  });
}

/** 企业号手机号登录：getPhoneNumber code → 后端解析手机号 */
export async function phoneLogin(code: string): Promise<AuthResult> {
  const url = getUrl("/auth/login");
  logger.info("AUTH", "请求手机号登录", { codeLen: code.length });
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method: "POST",
      header: { "Content-Type": "application/json" },
      data: { code },
      success: (res: any) => {
        if (res.statusCode === 200) {
          logger.info("AUTH", "手机号登录成功", { userId: res.data?.user_id });
          uni.setStorageSync("access_token", res.data.token);
          resolve(res.data as AuthResult);
        } else {
          logger.warn("AUTH", "手机号登录异常", { status: res.statusCode, detail: res.data?.detail });
          reject(new Error(res.data?.detail || "登录失败"));
        }
      },
      fail: (err) => {
        logger.error("AUTH", "手机号登录请求失败", { errMsg: err.errMsg });
        reject(new Error(err.errMsg || "请求失败"));
      },
    });
  });
}

export async function getMe(): Promise<UserProfile> {
  const url = getUrl("/auth/me");
  logger.info("AUTH", "请求用户信息");
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync("access_token");
    if (!token) {
      logger.warn("AUTH", "token 不存在，无法获取用户信息");
      return reject(new Error("未登录"));
    }
    uni.request({
      url,
      header: { Authorization: `Bearer ${token}` },
      success: (res: any) => {
        if (res.statusCode === 200) {
          logger.info("AUTH", "获取用户信息成功", { userId: res.data?.id });
          resolve(res.data as UserProfile);
        } else {
          logger.warn("AUTH", "获取用户信息失败", { status: res.statusCode });
          reject(new Error("获取用户信息失败"));
        }
      },
      fail: (err) => {
        logger.error("AUTH", "获取用户信息请求失败", { errMsg: err.errMsg });
        reject(new Error(err.errMsg || "请求失败"));
      },
    });
  });
}

export async function devLogin(phone: string): Promise<AuthResult> {
  const url = getUrl("/auth/dev-login");
  logger.info("AUTH", "请求测验登录", { phone });
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method: "POST",
      header: { "Content-Type": "application/json" },
      data: { phone },
      success: (res: any) => {
        if (res.statusCode === 200) {
          logger.info("AUTH", "测验登录成功", { userId: res.data?.user_id });
          uni.setStorageSync("access_token", res.data.token);
          resolve(res.data as AuthResult);
        } else {
          logger.warn("AUTH", "测验登录接口返回异常", { status: res.statusCode, detail: res.data?.detail });
          reject(new Error(res.data?.detail || "登录失败"));
        }
      },
      fail: (err) => {
        logger.error("AUTH", "测验登录请求失败", { errMsg: err.errMsg });
        reject(new Error(err.errMsg || "请求失败"));
      },
    });
  });
}

export function isLoggedIn(): boolean {
  return !!uni.getStorageSync("access_token");
}

export function logout() {
  uni.removeStorageSync("access_token");
}
