/**
 * 后端服务器地址，根据小程序运行环境自动切换。
 * - 正式版(release) → https://snaplearn.top
 * - 开发版/体验版   → http://localhost:8080
 *
 * 微信小程序要求服务器域名必须 HTTPS，并在小程序管理后台配置 request 合法域名。
 */

let _baseUrl = "";

function resolveBaseUrl(): string {
  try {
    const info = uni.getAccountInfoSync();
    const env = info.miniProgram?.envVersion || "unknown";
    // release=正式版  develop=开发版  trial=体验版
    // develop(DevTools) → localhost    trial/release(真机) → 线上地址
    return env === "develop" ? "http://localhost:8080" : "https://snaplearn.top";
  } catch {
    // 非小程序环境（H5等）默认本地地址
    return "http://localhost:8080";
  }
}

export function getServerBaseUrl(): string {
  return _baseUrl || (_baseUrl = resolveBaseUrl());
}

export function getApiBaseUrl(): string {
  return getServerBaseUrl() + "/api/v1";
}
