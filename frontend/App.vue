<template>
  <div class="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { logger } from "@/utils/logger";
import { getServerBaseUrl } from "@/config";

onLaunch(async () => {
  logger.setScene("app_start");
  let env = "unknown";
  try { env = uni.getAccountInfoSync()?.miniProgram?.envVersion || "unknown"; } catch {}
  logger.info("APP", "小程序启动", { env, server: getServerBaseUrl() });
  const userStore = useUserStore();
  await userStore.init();
  if (!userStore.isLoggedIn) {
    logger.info("APP", "用户未登录，跳转登录页");
    logger.flush();
    uni.reLaunch({ url: "/pages/login/login" });
  } else {
    logger.info("APP", "用户已登录", { nickname: userStore.nickname });
  }
});
</script>

<style lang="scss">
@import "@/styles/global.scss";
</style>
