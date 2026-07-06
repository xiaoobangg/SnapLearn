<template>
  <div class="login-page">
    <div class="login-wrapper">
      <div class="login-card">
        <div class="card-header">
          <div class="brand-logo"><div class="logo-circle"><el-icon><Odometer /></el-icon></div></div>
          <h1 style="font-size:22px;font-weight:700;color:#1F2937;margin:12px 0 0;">注册账号</h1>
        </div>
        <el-form :model="form" ref="formRef" label-position="top" class="login-form">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <el-input v-model="form.password" type="password" placeholder="至少6位" size="large" show-password :prefix-icon="Lock" />
          </div>
          <el-button type="primary" size="large" :loading="loading" @click="handleRegister" class="login-btn" :disabled="form.password.length < 6">
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form>
        <div class="card-footer">
          <span class="footer-text">已有账号？<router-link to="/login" style="color:#4D6BFE;">去登录</router-link></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAdminStore } from "@/store/admin";
import { ElMessage } from "element-plus";
import { User, Lock, Odometer } from "@element-plus/icons-vue";

const router = useRouter();
const adminStore = useAdminStore();
const loading = ref(false);
const form = reactive({ username: "", password: "" });

async function handleRegister() {
  if (!form.username.trim() || form.password.length < 6) return;
  loading.value = true;
  try {
    await adminStore.register(form.username.trim(), form.password);
    ElMessage.success("注册成功");
    router.push("/dashboard");
  } catch { /* handled by interceptor */ }
  loading.value = false;
}
</script>

<style lang="scss" scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: #F7F8FA; }
.login-wrapper { z-index: 10; }
.login-card { width: 400px; border-radius: 20px; background: #fff; border: 1px solid #E5E7EB; box-shadow: 0 12px 32px rgba(0,0,0,0.08); overflow: hidden; }
.card-header { padding: 36px 32px 8px; text-align: center; }
.logo-circle { width: 56px; height: 56px; border-radius: 16px; background: linear-gradient(135deg, #4D6BFE, #8B5CF6); display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 20px rgba(77,107,254,0.25); margin: 0 auto; .el-icon { font-size: 28px; color: #fff; } }
.login-form { padding: 16px 32px 24px; }
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 13px; font-weight: 600; color: #4B5563; margin-bottom: 8px; }
.login-btn { width: 100%; height: 48px; border-radius: 12px; font-size: 16px; font-weight: 600; background: #4D6BFE; border: none; box-shadow: 0 4px 12px rgba(77,107,254,0.25); margin-top: 8px; &:disabled { opacity: 0.5; } }
.card-footer { padding: 0 32px 24px; text-align: center; .footer-text { font-size: 13px; color: #9CA3AF; display: block; } }
</style>
