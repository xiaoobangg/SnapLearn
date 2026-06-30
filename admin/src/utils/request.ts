import axios from "axios";
import type { AxiosInstance, AxiosResponse } from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

const API_BASE = import.meta.env.VITE_APP_BASE_API;

const http: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
});

// 请求拦截：注入 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截：处理错误
http.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      switch (status) {
        case 401:
          localStorage.removeItem("admin_token");
          localStorage.removeItem("admin_info");
          if (router.currentRoute.value.path !== "/login") {
            router.push("/login");
          }
          ElMessage.error("登录已过期，请重新登录");
          break;
        case 403:
          ElMessage.error(data?.detail || "权限不足");
          break;
        default:
          ElMessage.error(data?.detail || "请求失败");
      }
    } else {
      ElMessage.error("网络错误，请检查连接");
    }
    return Promise.reject(error);
  }
);

export default http;
