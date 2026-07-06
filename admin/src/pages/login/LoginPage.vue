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
          
          <div class="form-group" v-if="showCaptcha">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="验证码"
                size="large"
                class="captcha-input"
                maxlength="4"
              />
              <img :src="captchaImage" @click="loadCaptcha" class="captcha-img" title="点击刷新" />
            </div>
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
          <span class="footer-text">还没有账号？<router-link to="/register" style="color:#4D6BFE;">立即注册</router-link></span>
          <span class="footer-text" style="margin-top:8px;">版本 v2.3</span>
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
  captchaCode: "",
});
const captchaKey = ref("");
const captchaImage = ref("");
const showCaptcha = ref(false);
const failCount = ref(parseInt(sessionStorage.getItem("login_fails") || "0"));

async function loadCaptcha() {
  try {
    const res = await http.get("/captcha");
    captchaKey.value = res.data.key;
    captchaImage.value = res.data.image;
  } catch { /* */ }
}

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const captchaK = showCaptcha.value ? captchaKey.value : undefined;
    const captchaC = showCaptcha.value ? form.captchaCode : undefined;
    const { is_default_pwd } = await adminStore.login(form.username, form.password, captchaK, captchaC);
    ElMessage.success("登录成功");
    sessionStorage.removeItem("login_fails");
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
      router.push(adminStore.role === "admin" ? "/dashboard" : "/documents");
    }
  } catch (e: any) {
    failCount.value++;
    sessionStorage.setItem("login_fails", String(failCount.value));
    if (failCount.value >= 2) {
      showCaptcha.value = true;
      loadCaptcha();
      form.captchaCode = "";
    }
  } finally {
    loading.value = false;
  }
}

async function doChangePwd() {
  try {
    await http.put("/admin/password", { old_password: oldPwd.value, new_password: newPwd.value });
    ElMessage.success("密码修改成功");
    showChangePwd.value = false;
    router.push(adminStore.role === "admin" ? "/dashboard" : "/documents");
  } catch { ElMessage.error("密码修改失败"); }
}

function skipChangePwd() {
  showChangePwd.value = false;
  router.push(adminStore.role === "admin" ? "/dashboard" : "/documents");
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
  background: #F7F8FA;
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
    radial-gradient(ellipse at 20% 20%, rgba(77, 107, 254, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(59, 130, 246, 0.04) 0%, transparent 70%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.25;
  animation: orbFloat 15s ease-in-out infinite;

  &.orb-1 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #4D6BFE, #8B5CF6);
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
  border-radius: 20px;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  box-shadow:
    0 12px 32px rgba(0, 0, 0, 0.08),
    0 4px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: cardAppear 0.6s ease-out;

  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #4D6BFE, #8B5CF6);
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
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #4D6BFE, #8B5CF6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(77, 107, 254, 0.25);
  animation: logoPulse 2s ease-in-out infinite;

  .el-icon {
    font-size: 28px;
    color: #fff;
  }
}

@keyframes logoPulse {
  0%, 100% {
    box-shadow: 0 8px 20px rgba(77, 107, 254, 0.25);
  }
  50% {
    box-shadow: 0 12px 28px rgba(77, 107, 254, 0.35);
  }
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: #1F2937;
    margin: 0;
    letter-spacing: 1px;
  }

  p {
    font-size: 13px;
    color: #6B7280;
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
  color: #4B5563;
  margin-bottom: 8px;
}

.form-input {
  .el-input__wrapper {
    border-radius: 10px;
    background: #FFFFFF;
    border-color: #E5E7EB;

    &:hover {
      border-color: #D1D5DB;
    }

    &.is-focus {
      border-color: #4D6BFE;
      box-shadow: 0 0 0 3px rgba(77, 107, 254, 0.12);
    }
  }

  .el-input__inner {
    color: #1F2937;

    &::placeholder {
      color: #9CA3AF;
    }
  }

  .el-input__prefix {
    color: #9CA3AF;
  }
}

.captcha-row {
  display: flex;
  gap: 10px;
  .captcha-input { flex: 1; }
  .captcha-img {
    width: 100px;
    height: 40px;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid #E5E7EB;
  }
}

.form-actions {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #4D6BFE;
  border: none;
  box-shadow: 0 4px 12px rgba(77, 107, 254, 0.25);
  transition: all 0.25s ease;

  &:hover:not(:disabled) {
    background: #3B5BDB;
    box-shadow: 0 6px 16px rgba(77, 107, 254, 0.35);
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
    color: #9CA3AF;
    font-weight: 500;
  }
}
</style>