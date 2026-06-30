<template>
  <div class="login-page">
    <div class="bg-effects">
      <div class="bg-gradient"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="grid-pattern"></div>
    </div>
    
    <div class="login-wrapper">
      <div class="login-card">
        <div class="card-header">
          <div class="brand-logo">
            <div class="logo-circle">
              <el-icon><Odometer /></el-icon>
            </div>
            <div class="brand-text">
              <h1>SnapLearn</h1>
              <p>智能英语学习管理系统</p>
            </div>
          </div>
        </div>
        
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <el-input 
              v-model="form.username" 
              placeholder="请输入用户名" 
              size="large"
              class="form-input"
              :prefix-icon="User"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">密码</label>
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              size="large"
              class="form-input"
              show-password
              :prefix-icon="Lock"
            />
          </div>
          
          <div class="form-actions">
            <el-button 
              type="primary" 
              size="large" 
              :loading="loading" 
              @click="handleLogin"
              class="login-btn"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </div>
        </el-form>
        
        <div class="card-footer">
          <span class="footer-text">版本 v2.3</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="showChangePwd" title="修改密码" width="420px">
      <el-form>
        <el-form-item label="旧密码">
          <el-input v-model="oldPwd" type="password" show-password class="form-input" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPwd" type="password" show-password placeholder="至少6位" class="form-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skipChangePwd">暂不修改</el-button>
        <el-button type="primary" :disabled="newPwd.length < 6" @click="doChangePwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAdminStore } from "@/store/admin";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/utils/request";
import type { FormInstance } from "element-plus";
import { Odometer, User, Lock } from "@element-plus/icons-vue";

const router = useRouter();
const adminStore = useAdminStore();
const formRef = ref<FormInstance>();
const loading = ref(false);

const showChangePwd = ref(false);
const oldPwd = ref("");
const newPwd = ref("");

const form = reactive({
  username: "",
  password: "",
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const { is_default_pwd } = await adminStore.login(form.username, form.password);
    ElMessage.success("登录成功");
    if (is_default_pwd) {
      ElMessageBox.confirm(
        "您当前使用的是默认密码，建议立即修改以确保安全。",
        "安全提示",
        { confirmButtonText: "去修改", cancelButtonText: "暂不修改", type: "warning" }
      ).then(() => {
        showChangePwd.value = true;
      }).catch(() => {});
    }
    if (!is_default_pwd) {
      router.push("/dashboard");
    }
  } catch (e: any) {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
}

async function doChangePwd() {
  try {
    await http.put("/admin/password", { old_password: oldPwd.value, new_password: newPwd.value });
    ElMessage.success("密码修改成功");
    showChangePwd.value = false;
    router.push("/dashboard");
  } catch { ElMessage.error("密码修改失败"); }
}

function skipChangePwd() {
  showChangePwd.value = false;
  router.push("/dashboard");
}
</script>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #0F172A;
}

.bg-effects {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(ellipse at 20% 20%, rgba(79, 70, 229, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(6, 182, 212, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(139, 92, 246, 0.1) 0%, transparent 70%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: orbFloat 15s ease-in-out infinite;
  
  &.orb-1 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #4F46E5, #8B5CF6);
    top: -100px;
    left: -100px;
    animation-delay: 0s;
  }
  
  &.orb-2 {
    width: 300px;
    height: 300px;
    background: linear-gradient(135deg, #06B6D4, #0EA5E9);
    bottom: -50px;
    right: -50px;
    animation-delay: 5s;
  }
  
  &.orb-3 {
    width: 250px;
    height: 250px;
    background: linear-gradient(135deg, #10B981, #34D399);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 10s;
  }
}

@keyframes orbFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(50px, -50px) scale(1.1);
  }
  66% {
    transform: translate(-30px, 30px) scale(0.9);
  }
}

.grid-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
  opacity: 0.3;
}

.login-wrapper {
  position: relative;
  z-index: 10;
  perspective: 1000px;
}

.login-card {
  width: 420px;
  border-radius: 24px;
  background: rgba(30, 41, 59, 0.85);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(79, 70, 229, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
  animation: cardAppear 0.6s ease-out;
  
  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(79, 70, 229, 0.5), rgba(6, 182, 212, 0.5), transparent);
  }
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(30px) rotateX(-10deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0);
  }
}

.card-header {
  padding: 36px 32px 24px;
  text-align: center;
}

.brand-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.logo-circle {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: linear-gradient(135deg, #4F46E5, #06B6D4);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 30px rgba(79, 70, 229, 0.5);
  animation: logoPulse 2s ease-in-out infinite;
  
  .el-icon {
    font-size: 32px;
    color: #fff;
  }
}

@keyframes logoPulse {
  0%, 100% {
    box-shadow: 0 0 30px rgba(79, 70, 229, 0.5);
  }
  50% {
    box-shadow: 0 0 45px rgba(79, 70, 229, 0.7), 0 0 70px rgba(6, 182, 212, 0.3);
  }
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  h1 {
    font-size: 24px;
    font-weight: 700;
    background: linear-gradient(135deg, #F8FAFC, #94A3B8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0;
    letter-spacing: 1px;
  }
  
  p {
    font-size: 13px;
    color: #64748B;
    margin: 0;
  }
}

.login-form {
  padding: 0 32px 24px;
}

.form-group {
  margin-bottom: 20px;
  
  &:last-of-type {
    margin-bottom: 24px;
  }
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #94A3B8;
  margin-bottom: 8px;
}

.form-input {
  .el-input__wrapper {
    border-radius: 12px;
    background: rgba(15, 23, 42, 0.6);
    border-color: rgba(255, 255, 255, 0.08);
    
    &:hover {
      border-color: rgba(79, 70, 229, 0.3);
    }
    
    &.is-focus {
      border-color: #4F46E5;
      box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
    }
  }
  
  .el-input__inner {
    color: #F8FAFC;
  }
  
  .el-input__prefix {
    color: #64748B;
  }
}

.form-actions {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #4F46E5, #3730A3);
  border: none;
  box-shadow: 
    0 4px 15px rgba(79, 70, 229, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  
  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #5B53D6, #3730A3);
    box-shadow: 
      0 6px 25px rgba(79, 70, 229, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.1);
    transform: translateY(-1px);
  }
  
  &:active:not(:disabled) {
    transform: translateY(0);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.card-footer {
  padding: 16px 32px 24px;
  text-align: center;
  
  .footer-text {
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
}
</style>