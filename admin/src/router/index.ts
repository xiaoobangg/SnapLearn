import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/pages/login/LoginPage.vue"),
    meta: { noAuth: true },
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/pages/login/RegisterPage.vue"),
    meta: { noAuth: true },
  },
  {
    path: "/",
    component: () => import("@/layouts/AdminLayout.vue"),
    redirect: "/blog",
    children: [
      {
        path: "blog",
        name: "Blog",
        component: () => import("@/pages/blog/BlogListPage.vue"),
        meta: { title: "博客", noAuth: true },
      },
      {
        path: "blog/:id",
        name: "BlogDetail",
        component: () => import("@/pages/blog/BlogDetailPage.vue"),
        meta: { title: "文章详情", noAuth: true },
      },
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/pages/dashboard/DashboardPage.vue"),
        meta: { title: "仪表盘" },
      },
      {
        path: "users",
        name: "Users",
        component: () => import("@/pages/users/UserListPage.vue"),
        meta: { title: "用户管理", adminOnly: true },
      },
      {
        path: "users/:id",
        name: "UserDetail",
        component: () => import("@/pages/users/UserDetailPage.vue"),
        meta: { title: "用户详情" },
      },
      {
        path: "groups",
        name: "Groups",
        component: () => import("@/pages/groups/GroupListPage.vue"),
        meta: { title: "卡片组管理" },
      },
      {
        path: "groups/:id",
        name: "GroupDetail",
        component: () => import("@/pages/groups/GroupDetailPage.vue"),
        meta: { title: "卡片组详情" },
      },
      {
        path: "cards",
        name: "Cards",
        component: () => import("@/pages/cards/CardListPage.vue"),
        meta: { title: "卡片管理" },
      },
      {
        path: "word-banks",
        name: "WordBanks",
        component: () => import("@/pages/wordbanks/WordBankPage.vue"),
        meta: { title: "词库管理" },
      },
      {
        path: "word-contents",
        name: "WordContents",
        component: () => import("@/pages/wordcontents/WordContentPage.vue"),
        meta: { title: "单词内容" },
      },
      {
        path: "knowledge",
        name: "Knowledge",
        component: () => import("@/pages/knowledge/KnowledgePage.vue"),
        meta: { title: "知识库" },
      },
      {
        path: "documents",
        name: "Documents",
        component: () => import("@/pages/documents/DocumentManagePage.vue"),
        meta: { title: "文档管理" },
      },
      {
        path: "logs",
        name: "Logs",
        component: () => import("@/pages/logs/AccessLogPage.vue"),
        meta: { title: "访问日志", adminOnly: true },
      },
      {
        path: "api-keys",
        name: "ApiKeys",
        component: () => import("@/pages/api-keys/ApiKeyListPage.vue"),
        meta: { title: "API Key 管理", adminOnly: true },
      },
      {
        path: "voices",
        name: "Voices",
        component: () => import("@/pages/voices/VoiceListPage.vue"),
        meta: { title: "音色管理" },
      },
      {
        path: "voices/clone",
        name: "VoiceClone",
        component: () => import("@/pages/voices/VoiceClonePage.vue"),
        meta: { title: "声音复刻" },
      },
      {
        path: "chat",
        name: "Chat",
        component: () => import("@/pages/chat/ChatPage.vue"),
        meta: { title: "AI 对话" },
      },
      {
        path: "chat-traces",
        name: "ChatTraces",
        component: () => import("@/pages/chat-traces/ChatTraceListPage.vue"),
        meta: { title: "AI 对话日志" },
      },
      {
        path: "feedbacks",
        name: "Feedbacks",
        component: () => import("@/pages/feedbacks/FeedbackPage.vue"),
        meta: { title: "用户反馈" },
      },
      {
        path: "settings",
        name: "Settings",
        component: () => import("@/pages/settings/SettingsPage.vue"),
        meta: { title: "系统设置" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("admin_token");
  if (to.meta.noAuth || to.path === "/") {
    if (token && to.path === "/login") {
      next("/blog");
    } else {
      next();
    }
  } else {
    if (!token) {
      next("/login");
    } else {
      // 检查 adminOnly 路由权限
      if (to.meta.adminOnly) {
        const adminInfo = localStorage.getItem("admin_info");
        if (adminInfo) {
          const admin = JSON.parse(adminInfo);
          if (admin.role !== "admin") {
            next("/blog");
            return;
          }
        }
      }
      next();
    }
  }
});

export default router;
