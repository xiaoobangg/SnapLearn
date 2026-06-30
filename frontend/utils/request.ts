import { getApiBaseUrl } from "@/config";

interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: any;
  header?: Record<string, string>;
  auth?: boolean;
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  const fullUrl = `${getApiBaseUrl()}${options.url}`;
  const method = options.method || "GET";

  console.log(`[API] ${method} ${fullUrl}`, options.data || "");

  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync("access_token");
    const header: Record<string, string> = {
      "Content-Type": "application/json",
      ...options.header,
    };

    if (options.auth !== false) {
      if (!token) {
        uni.reLaunch({ url: "/pages/login/login" });
        reject(new Error("NOT_LOGIN"));
        return;
      }
      header["Authorization"] = `Bearer ${token}`;
    }

    uni.request({
      url: fullUrl,
      method,
      data: options.data,
      header,
      success: (res: any) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          console.log(`[API] ${method} ${fullUrl} -> ${res.statusCode}`, res.data);
          resolve(res.data as T);
        } else if (res.statusCode === 401) {
          console.warn(`[API] ${method} ${fullUrl} -> 401`, res.data);
          uni.removeStorageSync("access_token");
          uni.reLaunch({ url: "/pages/login/login" });
          reject(new Error("NOT_LOGIN"));
        } else {
          console.warn(`[API] ${method} ${fullUrl} -> ${res.statusCode}`, res.data);
          reject(new Error(res.data?.detail || "请求失败"));
        }
      },
      fail: (err) => {
        console.error(`[API] ${method} ${fullUrl} -> fail`, err);
        reject(new Error(err.errMsg || "网络请求失败"));
      },
    });
  });
}

export function uploadFile(filePath: string): Promise<{ url: string }> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync("access_token");
    uni.uploadFile({
      url: `${getApiBaseUrl()}/storage/upload`,
      filePath,
      name: "file",
      header: {
        Authorization: `Bearer ${token}`,
      },
      success: (res: any) => {
        if (res.statusCode === 200) {
          resolve(JSON.parse(res.data));
        } else {
          reject(new Error("上传失败"));
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || "上传失败"));
      },
    });
  });
}
