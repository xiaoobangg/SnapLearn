<template>
  <view class="login-page">
    <view class="deco-star top-left">&#x2B50;</view>
    <view class="deco-star top-right">&#x2728;</view>
    <view class="deco-circle bottom-right"></view>
    <view class="deco-circle bottom-left"></view>
    <view class="hero-characters">
      <text class="char-icon">&#x1F430;</text>
      <text class="char-icon char-2">&#x1F43C;</text>
      <text class="char-icon char-3">&#x1F981;</text>
    </view>
    <text class="app-name">拍立学</text>
    <text class="app-desc">拍照识词 · 快乐学英语</text>
    <view class="login-actions">
      <button class="btn-login" :loading="loading" @click="handleWechatLogin">
        <text>&#x1F4DE; 微信登录</text>
      </button>
    </view>
    <view class="dev-login" v-if="isDev">
      <text class="dev-toggle" @click="showDev = !showDev">&#x1F3AD; 本地测验登录</text>
      <view class="dev-form" v-if="showDev">
        <input class="dev-input" v-model="devPhone" placeholder="输入手机号" type="number" maxlength="11" />
        <button class="btn-dev-login" :loading="devLoading" @click="handleDevLogin">测验登录</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useUserStore } from "@/store/user";
import * as authUtil from "@/utils/auth";
import { logger } from "@/utils/logger";

// 仅在开发版/体验版显示"本地测验登录"按钮（正式版隐藏）
const isDev = computed(() => {
  try {
    const info = uni.getAccountInfoSync();
    const env = info.miniProgram?.envVersion || "unknown";
    logger.info("LOGIN", "当前小程序环境", { env, appId: info.miniProgram?.appId || "unknown" });
    // develop=开发版(DevTools)  trial=体验版  release=正式版
    return env === "develop";
  } catch {
    logger.info("LOGIN", "非小程序环境，默认显示测验登录");
    return true;
  }
});

const userStore = useUserStore();
const loading = ref(false);
const showDev = ref(false);
const devPhone = ref("");
const devLoading = ref(false);

async function handleWechatLogin() {
  logger.setScene("wx_login");
  logger.info("LOGIN", "用户点击微信登录");
  loading.value = true;
  try {
    await userStore.loginWithWechat();
    logger.info("LOGIN", "微信登录成功");
    logger.flush();
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(() => uni.switchTab({ url: "/pages/index/index" }), 800);
  } catch (_e: any) {
    logger.error("LOGIN", "微信登录失败", { msg: _e.message });
    logger.flush();
    uni.showToast({ title: _e.message || "登录失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

async function handleDevLogin() {
  logger.setScene("dev_login");
  if (!devPhone.value || devPhone.value.length < 11) {
    logger.warn("LOGIN", "测验登录手机号不合法", { phone: devPhone.value });
    uni.showToast({ title: "请输入11位手机号", icon: "none" });
    return;
  }
  logger.info("LOGIN", "用户点击测验登录", { phone: devPhone.value });
  devLoading.value = true;
  try {
    const result = await authUtil.devLogin(devPhone.value);
    uni.setStorageSync("access_token", result.token);
    userStore.isLoggedIn = true;
    userStore.userId = result.user_id;
    userStore.nickname = result.nickname;
    logger.info("LOGIN", "测验登录成功", { userId: result.user_id });
    logger.flush();
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(() => uni.switchTab({ url: "/pages/index/index" }), 800);
  } catch (_e: any) {
    logger.error("LOGIN", "测验登录失败", { msg: _e.message });
    logger.flush();
    uni.showToast({ title: _e.message || "登录失败", icon: "none" });
  } finally {
    devLoading.value = false;
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: $gradient-sunset; padding: 64rpx; position: relative; overflow: hidden;
}
.deco-star { position: absolute; font-size: 64rpx; opacity: 0.6; animation: wiggle 3s ease-in-out infinite;
  &.top-left { top: 100rpx; left: 60rpx; animation-delay: 0s; }
  &.top-right { top: 140rpx; right: 80rpx; animation-delay: 1s; }
}
.deco-circle { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.06); pointer-events: none;
  &.bottom-right { bottom: -120rpx; right: -80rpx; width: 500rpx; height: 500rpx; }
  &.bottom-left { bottom: -60rpx; left: -60rpx; width: 300rpx; height: 300rpx; background: rgba(255,255,255,0.04); }
}
.hero-characters { display: flex; align-items: center; gap: 16rpx; margin-bottom: 32rpx; position: relative; z-index: 1;
  .char-icon { font-size: 80rpx; animation: bounce 2s ease-in-out infinite;
    &.char-2 { font-size: 100rpx; animation-delay: 0.3s; }
    &.char-3 { font-size: 80rpx; animation-delay: 0.6s; }
  }
}
.app-name { font-size: 64rpx; font-weight: 800; color: #fff; letter-spacing: 8rpx; text-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1); z-index: 1; }
.app-desc { font-size: $font-base; color: rgba(255,255,255,0.75); margin-top: $spacing-sm; z-index: 1; }
.login-actions { display: flex; flex-direction: column; align-items: center; width: 100%; z-index: 1; }
.btn-login { width: 100%; max-width: 560rpx; background: rgba(255,255,255,0.95); color: $text-primary; border: none; border-radius: $radius-pill; padding: 28rpx; font-size: $font-lg; font-weight: $font-weight-bold; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.1); transition: $transition-fast; &:active { transform: scale(0.97); } }
.login-tip { font-size: $font-sm; color: rgba(255,255,255,0.55); margin-top: $spacing-lg; }
.dev-login { margin-top: 60rpx; width: 100%; max-width: 560rpx; z-index: 1; }
.dev-toggle { font-size: $font-sm; color: rgba(255,255,255,0.4); text-align: center; display: block; padding: 8rpx; }
.dev-form { margin-top: $spacing-md; display: flex; gap: $spacing-base; }
.dev-input { flex: 1; height: 76rpx; background: rgba(255,255,255,0.9); border-radius: $radius-pill; padding: 0 $spacing-xl; font-size: $font-base; color: $text-primary; }
.btn-dev-login { background: rgba(255,255,255,0.2); backdrop-filter: blur(8rpx); color: #fff; border: 2rpx solid rgba(255,255,255,0.3); border-radius: $radius-pill; padding: $spacing-base $spacing-xl; font-size: $font-base; font-weight: $font-weight-medium; white-space: nowrap; }
</style>
