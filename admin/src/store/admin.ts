import { defineStore } from "pinia";
import { ref } from "vue";
import { authApi } from "@/api";

export const useAdminStore = defineStore("admin", () => {
  const id = ref("");
  const username = ref("");
  const email = ref("");
  const role = ref("");

  const isLoggedIn = ref(!!localStorage.getItem("admin_token"));

  async function login(usernameVal: string, password: string, captchaKey?: string, captchaCode?: string) {
    const res = await authApi.login(usernameVal, password, captchaKey, captchaCode);
    const { token, admin, is_default_pwd } = res.data;
    localStorage.setItem("admin_token", token);
    localStorage.setItem("admin_info", JSON.stringify(admin));
    id.value = admin.id;
    username.value = admin.username;
    email.value = "";
    role.value = admin.role;
    isLoggedIn.value = true;
    return { admin, is_default_pwd: !!is_default_pwd };
  }

  async function register(usernameVal: string, password: string) {
    const res = await authApi.register(usernameVal, password);
    const { token, user } = res.data;
    localStorage.setItem("admin_token", token);
    localStorage.setItem("admin_info", JSON.stringify(user));
    id.value = user.id;
    username.value = user.username;
    role.value = user.role;
    isLoggedIn.value = true;
  }

  function loadFromStorage() {
    const stored = localStorage.getItem("admin_info");
    if (stored) {
      const admin = JSON.parse(stored);
      id.value = admin.id;
      username.value = admin.username;
      email.value = admin.email;
      role.value = admin.role;
      isLoggedIn.value = true;
    }
  }

  function logout() {
    localStorage.removeItem("admin_token");
    localStorage.removeItem("admin_info");
    id.value = "";
    username.value = "";
    email.value = "";
    role.value = "";
    isLoggedIn.value = false;
  }

  return { id, username, email, role, isLoggedIn, login, register, loadFromStorage, logout };
});
