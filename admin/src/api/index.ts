import http from "@/utils/request";

// ===== 认证 =====
export const authApi = {
  login(username: string, password: string) {
    return http.post("/admin/login", { username, password });
  },
  me() {
    return http.get("/admin/me");
  },
};

// ===== 仪表盘 =====
export const dashboardApi = {
  overview() {
    return http.get("/admin/dashboard");
  },
  dailyStats(days = 7) {
    return http.get("/admin/stats/daily", { params: { days } });
  },
};

// ===== 用户 =====
export const userApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return http.get("/admin/users", { params });
  },
  detail(id: string) {
    return http.get(`/admin/users/${id}`);
  },
};

// ===== 卡片组 =====
export const groupApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return http.get("/admin/groups", { params });
  },
  detail(id: string) {
    return http.get(`/admin/groups/${id}`);
  },
  delete(id: string) {
    return http.delete(`/admin/groups/${id}`);
  },
};

// ===== 卡片 =====
export const cardApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return http.get("/admin/cards", { params });
  },
};

// ===== 词库管理（新增） =====
export const wordBankApi = {
  list(params: { page?: number; pageSize?: number }) {
    return http.get("/admin/word-banks", { params });
  },
  create(name: string, description?: string) {
    return http.post("/admin/word-banks", { name, description });
  },
};

// ===== 单词内容管理（新增） =====
export const wordContentApi = {
  list(params: { page?: number; pageSize?: number; keyword?: string }) {
    return http.get("/admin/word-contents", { params });
  },
  refresh(wordId: string) {
    return http.post(`/admin/word-contents/${wordId}/refresh`);
  },
};

// ===== AI 对话日志 =====
export const chatTraceApi = {
  list(params: { page?: number; pageSize?: number; userId?: string; chatId?: string; status?: string; minDurationMs?: number; maxDurationMs?: number }) {
    return http.get("/admin/chat-traces", { params });
  },
  detail(id: string) {
    return http.get(`/admin/chat-traces/${id}`);
  },
};

// ===== 音色管理 =====
export const voiceApi = {
  list() { return http.get("/admin/voices"); },
  catalog() { return http.get("/admin/voices/catalog"); },
  importVoices(voices: any[]) { return http.post("/admin/voices/import", voices); },
  create(data: any) { return http.post("/admin/voices", data); },
  update(id: string, data: any) { return http.put(`/admin/voices/${id}`, data); },
  delete(id: string) { return http.delete(`/admin/voices/${id}`); },
  setDefault(id: string) { return http.post(`/admin/voices/${id}/default`); },
  test(id: string, text: string) { return http.post(`/admin/voices/test/${id}`, { text }); },
  listEnrolled() { return http.get("/admin/voices/enroll"); },
  importEnrolled(data: any) { return http.post("/admin/voices/enroll/import", data); },
  deleteEnrolled(voiceCode: string) { return http.delete(`/admin/voices/enroll/${voiceCode}`); },
};
