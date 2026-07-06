<template>
  <el-container class="layout">
    <el-aside width="240px" class="aside">
      <div class="logo-wrapper">
        <div class="logo">
          <div class="logo-icon">
            <el-icon><Odometer /></el-icon>
          </div>
          <span class="logo-text">SnapLearn</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#6B7280"
        active-text-color="#4D6BFE"
        class="main-menu"
      >
        <el-menu-item index="/dashboard" v-if="isAdmin">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/blog">
          <el-icon><Notebook /></el-icon>
          <span>博客</span>
        </el-menu-item>
        <template v-if="isAdmin">
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/groups">
            <el-icon><Folder /></el-icon>
            <span>卡片组管理</span>
          </el-menu-item>
          <el-menu-item index="/cards">
            <el-icon><Document /></el-icon>
            <span>卡片管理</span>
          </el-menu-item>
          <el-menu-item index="/word-banks">
            <el-icon><Collection /></el-icon>
            <span>词库管理</span>
          </el-menu-item>
          <el-menu-item index="/word-contents">
            <el-icon><Notebook /></el-icon>
            <span>单词内容</span>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Notebook /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="/voices">
            <el-icon><Microphone /></el-icon>
            <span>音色管理</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 对话</span>
          </el-menu-item>
          <el-menu-item index="/logs">
            <el-icon><List /></el-icon>
            <span>访问日志</span>
          </el-menu-item>
          <el-menu-item index="/chat-traces">
            <el-icon><ChatLineRound /></el-icon>
            <span>AI 对话日志</span>
          </el-menu-item>
          <el-menu-item index="/api-keys">
            <el-icon><Key /></el-icon>
            <span>API Key</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="aside-footer">
        <div class="version">v2.3</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="page-title">{{ route.meta.title || "" }}</div>
          <div class="page-breadcrumb">
            <span class="breadcrumb-item">首页</span>
            <span class="breadcrumb-separator">/</span>
            <span class="breadcrumb-item active">{{ route.meta.title || "" }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <div class="status-indicator">
              <span class="status-dot"></span>
              <span class="status-text">在线</span>
            </div>
          </div>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <div class="user-avatar">
                <el-icon><User /></el-icon>
              </div>
              <div class="user-detail">
                <span class="user-name">{{ adminStore.username }}</span>
                <span class="user-role">{{ isAdmin ? '管理员' : '普通用户' }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>

      <!-- 修改密码对话框 -->
      <el-dialog v-model="showPwdDialog" title="修改密码" width="400px">
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
          <el-form-item label="旧密码" prop="oldPwd">
            <el-input v-model="pwdForm.oldPwd" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPwd">
            <el-input v-model="pwdForm.newPwd" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPwd">
            <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showPwdDialog=false">取消</el-button>
          <el-button type="primary" @click="doChangePwd">确认修改</el-button>
        </template>
      </el-dialog>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAdminStore } from "@/store/admin";
import {
  Odometer,
  User,
  Folder,
  Document,
  Collection,
  Notebook,
  Microphone,
  ChatDotRound,
  List,
  ChatLineRound,
  Key,
  Setting,
  ArrowDown,
} from "@element-plus/icons-vue";
import http from "@/utils/request";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const isAdmin = computed(() => adminStore.role === "admin");

const activeMenu = computed(() => {
  const path = route.path;
  if (path.startsWith("/users")) return "/users";
  if (path.startsWith("/groups")) return "/groups";
  if (path.startsWith("/cards")) return "/cards";
  if (path.startsWith("/word-banks")) return "/word-banks";
  if (path.startsWith("/word-contents")) return "/word-contents";
  if (path.startsWith("/knowledge")) return "/knowledge";
  if (path.startsWith("/documents")) return "/documents";
  if (path.startsWith("/blog")) return "/blog";
  if (path.startsWith("/chat-traces")) return "/chat-traces";
  if (path.startsWith("/chat")) return "/chat";
  if (path.startsWith("/voices")) return "/voices";
  if (path.startsWith("/api-keys")) return "/api-keys";
  if (path.startsWith("/logs")) return "/logs";
  if (path.startsWith("/chat-traces")) return "/chat-traces";
  return path;
});

const showPwdDialog = ref(false);
const pwdFormRef = ref();
const pwdForm = reactive({ oldPwd: "", newPwd: "", confirmPwd: "" });
const pwdRules = {
  oldPwd: [{ required: true, message: "请输入旧密码", trigger: "blur" }],
  newPwd: [{ required: true, min: 6, message: "至少6位", trigger: "blur" }],
  confirmPwd: [{ required: true, validator: (_r: any, v: string, cb: any) => cb(v !== pwdForm.newPwd ? new Error("两次密码不一致") : undefined), trigger: "blur" }],
};

function handleCommand(cmd: string) {
  if (cmd === "changePwd") { pwdForm.oldPwd = ""; pwdForm.newPwd = ""; pwdForm.confirmPwd = ""; showPwdDialog.value = true; }
  if (cmd === "logout") { adminStore.logout(); router.push("/login"); }
}

async function doChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  try {
    await http.put("/admin/password", { old_password: pwdForm.oldPwd, new_password: pwdForm.newPwd });
    ElMessage.success("密码修改成功，请重新登录");
    showPwdDialog.value = false;
    adminStore.logout();
    router.push("/login");
  } catch { ElMessage.error("密码修改失败"); }
}
</script>

<style lang="scss" scoped>
.layout {
  height: 100vh;
  background: #F7F8FA;
}

.aside {
  background: #FFFFFF;
  border-right: 1px solid #E5E7EB;
  overflow: hidden;
  position: relative;
}

.logo-wrapper {
  padding: 20px 20px;
  border-bottom: 1px solid #E5E7EB;
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .logo-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: linear-gradient(135deg, #4D6BFE, #8B5CF6);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;
    box-shadow: 0 4px 12px rgba(77, 107, 254, 0.25);

    .el-icon {
      font-size: 22px;
      color: #fff;
    }
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: #1F2937;
    letter-spacing: 1px;
  }

  .logo-sub {
    font-size: 11px;
    color: #9CA3AF;
    letter-spacing: 2px;
    margin-top: 2px;
  }
}

.main-menu {
  padding: 12px 8px;

  .el-menu-item {
    margin: 4px 8px;
    border-radius: 8px;
    padding: 10px 14px;
    transition: all 0.2s ease;
    position: relative;
    color: #6B7280;

    &:hover {
      background: #F3F4F6;
      color: #1F2937;

      .el-icon {
        color: #4D6BFE;
      }
    }

    &.is-active {
      background: #EEF2FF;
      color: #4D6BFE;
      font-weight: 600;

      .el-icon {
        color: #4D6BFE;
      }
    }

    .el-icon {
      font-size: 18px;
      margin-right: 12px;
      transition: color 0.2s ease;
    }

    span {
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.aside-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 16px;
  border-top: 1px solid #E5E7EB;
  background: #FFFFFF;

  .version {
    text-align: center;
    font-size: 12px;
    color: #9CA3AF;
    font-weight: 500;
  }
}

.header {
  background: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 60px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .page-title {
    font-size: 18px;
    font-weight: 700;
    color: #1F2937;
    letter-spacing: 0.3px;
  }

  .page-breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;

    .breadcrumb-item {
      font-size: 12px;
      color: #9CA3AF;

      &.active {
        color: #6B7280;
      }
    }

    .breadcrumb-separator {
      color: #9CA3AF;
      font-size: 12px;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 20px;
  background: rgba(16, 185, 129, 0.1);

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #10B981;
    animation: statusBlink 2s ease-in-out infinite;
  }

  .status-text {
    font-size: 12px;
    color: #047857;
    font-weight: 500;
  }
}

@keyframes statusBlink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 10px;
  transition: background 0.2s ease;

  &:hover {
    background: #F3F4F6;
  }

  .user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4D6BFE, #8B5CF6);
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 16px;
      color: #fff;
    }
  }

  .user-detail {
    display: flex;
    flex-direction: column;
    gap: 1px;

    .user-name {
      font-size: 13px;
      font-weight: 600;
      color: #1F2937;
    }

    .user-role {
      font-size: 11px;
      color: #9CA3AF;
    }
  }

  .el-icon {
    font-size: 14px;
    color: #9CA3AF;
  }
}

.main {
  background: #F7F8FA;
  padding: 24px;
  overflow-y: auto;
  height: 0;
  flex: 1;
}
</style>