<template>
  <el-container class="layout">
    <el-aside width="240px" class="aside">
      <div class="logo-wrapper">
        <div class="logo">
          <div class="logo-icon">
            <el-icon><Odometer /></el-icon>
          </div>
          <span class="logo-text">SnapLearn</span>
          <span class="logo-sub">管理后台</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#94A3B8"
        active-text-color="#fff"
        class="main-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
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
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
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
  if (path.startsWith("/chat-traces")) return "/chat-traces";
  if (path.startsWith("/chat")) return "/chat";
  if (path.startsWith("/voices")) return "/voices";
  if (path.startsWith("/api-keys")) return "/api-keys";
  if (path.startsWith("/logs")) return "/logs";
  if (path.startsWith("/chat-traces")) return "/chat-traces";
  return path;
});

function handleCommand(cmd: string) {
  if (cmd === "logout") {
    adminStore.logout();
    router.push("/login");
  }
}
</script>

<style lang="scss" scoped>
.layout {
  height: 100vh;
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%);
}

.aside {
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  position: relative;
  
  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(79, 70, 229, 0.5), transparent);
  }
  
  &::after {
    content: "";
    position: absolute;
    top: 0;
    right: 0;
    width: 1px;
    height: 100%;
    background: linear-gradient(180deg, rgba(79, 70, 229, 0.3), transparent);
  }
}

.logo-wrapper {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  
  .logo-icon {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    background: linear-gradient(135deg, #4F46E5, #06B6D4);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;
    box-shadow: 0 0 24px rgba(79, 70, 229, 0.4);
    animation: logoPulse 3s ease-in-out infinite;
    
    .el-icon {
      font-size: 24px;
      color: #fff;
    }
  }
  
  .logo-text {
    font-size: 18px;
    font-weight: 700;
    background: linear-gradient(135deg, #F8FAFC, #94A3B8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 1px;
  }
  
  .logo-sub {
    font-size: 11px;
    color: #64748B;
    letter-spacing: 2px;
    margin-top: 2px;
  }
}

@keyframes logoPulse {
  0%, 100% {
    box-shadow: 0 0 24px rgba(79, 70, 229, 0.4);
  }
  50% {
    box-shadow: 0 0 36px rgba(79, 70, 229, 0.6), 0 0 60px rgba(6, 182, 212, 0.2);
  }
}

.main-menu {
  padding: 16px 8px;
  
  .el-menu-item {
    margin: 6px 8px;
    border-radius: 12px;
    padding: 12px 16px;
    transition: all 0.25s ease;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      background: linear-gradient(180deg, #4F46E5, #06B6D4);
      border-radius: 0 3px 3px 0;
      transition: height 0.25s ease;
    }
    
    &:hover {
      background: rgba(79, 70, 229, 0.12);
      color: #F8FAFC;
      
      .el-icon {
        color: #4F46E5;
      }
      
      &::before {
        height: 60%;
      }
    }
    
    &.is-active {
      background: rgba(79, 70, 229, 0.2);
      color: #fff;
      
      .el-icon {
        color: #4F46E5;
      }
      
      &::before {
        height: 60%;
      }
      
      &::after {
        content: "";
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: radial-gradient(circle at 10% 50%, rgba(79, 70, 229, 0.15), transparent 60%);
      }
    }
    
    .el-icon {
      font-size: 18px;
      margin-right: 12px;
      transition: color 0.25s ease;
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
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  
  .version {
    text-align: center;
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
}

.header {
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 64px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .page-title {
    font-size: 18px;
    font-weight: 700;
    color: #F8FAFC;
    letter-spacing: 0.5px;
  }
  
  .page-breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .breadcrumb-item {
      font-size: 12px;
      color: #64748B;
      
      &.active {
        color: #94A3B8;
      }
    }
    
    .breadcrumb-separator {
      color: #64748B;
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
  padding: 6px 12px;
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
    color: #10B981;
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
  border-radius: 12px;
  transition: background 0.25s ease;
  
  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .user-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4F46E5, #8B5CF6);
    display: flex;
    align-items: center;
    justify-content: center;
    
    .el-icon {
      font-size: 18px;
      color: #fff;
    }
  }
  
  .user-detail {
    display: flex;
    flex-direction: column;
    gap: 1px;
    
    .user-name {
      font-size: 14px;
      font-weight: 600;
      color: #F8FAFC;
    }
    
    .user-role {
      font-size: 11px;
      color: #64748B;
    }
  }
  
  .el-icon {
    font-size: 14px;
    color: #94A3B8;
  }
}

.main {
  background: transparent;
  padding: 24px;
  overflow-y: auto;
}
</style>