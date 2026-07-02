<template>
  <view class="profile">
    <view class="not-login" v-if="!userStore.isLoggedIn">
      <view class="nl-avatar-wrap"><text>👤</text></view>
      <button class="btn-login" @click="goLogin">🔒 点击登录</button>
    </view>
    <template v-else>
      <view class="user-header">
        <view class="avatar-wrap" @click="changeAvatar">
          <image v-if="userStore.avatarUrl" class="avatar" :src="avatarSrc" mode="aspectFill" />
          <view v-else class="avatar avatar-placeholder"><text>👤</text></view>
          <view class="avatar-overlay"><text>📷</text></view>
        </view>
        <text class="nickname" @click="showNicknameEdit = true">{{ userStore.nickname || '学习者' }}</text>
        <text class="level-badge">📚 英语小达人</text>
      </view>
      <view class="stats-row">
        <view class="stat-item"><text class="stat-num">{{ checkinStats.consecutive_days || 0 }}</text><text class="stat-label">🔥 连续打卡</text></view>
        <view class="stat-divider" />
        <view class="stat-item"><text class="stat-num">{{ checkinStats.total_checkin_days || 0 }}</text><text class="stat-label">累计打卡</text></view>
        <view class="stat-divider" />
        <view class="stat-item"><text class="stat-num">{{ checkinStats.total_pool_words || 0 }}</text><text class="stat-label">📚 单词总量</text></view>
      </view>
      <view class="stats-row secondary">
        <view class="stat-item"><text class="stat-num">{{ learnStats.totalGroups }}</text><text class="stat-label">卡片组</text></view>
        <view class="stat-divider" />
        <view class="stat-item"><text class="stat-num success">{{ learnStats.testDone }}</text><text class="stat-label">🎉 已通关</text></view>
        <view class="stat-divider" />
        <view class="stat-item"><text class="stat-num">{{ checkinStats.mastered_count || 0 }}</text><text class="stat-label">已掌握</text></view>
      </view>
    </template>
    <view class="menu-list">
      <view class="menu-item" @click="goCheckinShare"><text>📸 打卡分享</text><view class="menu-right"><text>›</text></view></view>
<view class="menu-item" @click="goCheckinCalendar"><text>📅 打卡日历</text><view class="menu-right"><text class="menu-badge" v-if="checkinStats.consecutive_days > 0">连续{{ checkinStats.consecutive_days }}天</text><text>›</text></view></view>
      <!-- <view class="menu-item" @click="goAIChat"><text>🤖 AI 助手</text><text>›</text></view>-->
      <view class="menu-item" @click="showAbout"><text>ℹ️ 关于拍立学</text><text>›</text></view>
      <view class="menu-item" @click="handleLogout" v-if="userStore.isLoggedIn"><text class="danger">🚪 退出登录</text><text>›</text></view>
    </view>

    <!-- 昵称编辑弹窗 -->
    <view class="modal-mask" v-if="showNicknameEdit" @click="showNicknameEdit = false">
      <view class="modal-box" @click.stop>
        <text class="modal-title">修改昵称</text>
        <input class="nickname-input" v-model="editNickname" placeholder="输入新昵称" maxlength="20" />
        <view class="modal-actions">
          <button class="btn-cancel" @click="showNicknameEdit = false">取消</button>
          <button class="btn-save" :disabled="!editNickname.trim()" @click="saveNickname">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { logger } from "@/utils/logger";
import { getServerBaseUrl } from "@/config";
import { api } from "@/api";
const userStore = useUserStore();
const checkinStats = ref({ total_checkin_days: 0, consecutive_days: 0, total_pool_words: 0, mastered_count: 0 });
const learnStats = ref({ totalGroups: 0, testDone: 0 });
const showNicknameEdit = ref(false);
const editNickname = ref("");

// DB 存相对路径，显示时按当前域名自动拼完整 URL
const avatarSrc = computed(() => {
  const url = userStore.avatarUrl;
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return getServerBaseUrl() + url;
});

onShow(async () => {
  if (userStore.isLoggedIn) {
    try { const [stats, groupList] = await Promise.all([api.getCheckinStats(), api.listCardGroups(true)]); checkinStats.value = stats; const groups = groupList || []; learnStats.value.totalGroups = groups.length; learnStats.value.testDone = groups.filter((g: any) => g.group_status === "test_done").length; } catch (_e) { }
  }
});

async function changeAvatar() {
  try {
    const res = await uni.chooseImage({ count: 1, sizeType: ["compressed"], sourceType: ["album", "camera"] });
    const filePath = res.tempFilePaths[0];
    if (!filePath) return;
    uni.showLoading({ title: "上传中..." });
    const relativeUrl = await userStore.uploadAvatar(filePath);
    // DB 只存相对路径，显示时再拼域名
    await userStore.updateProfile({ avatar_url: relativeUrl });
    uni.hideLoading();
    uni.showToast({ title: "头像已更新", icon: "success" });
    logger.info("USER", "头像更新成功", { url: relativeUrl });
  } catch (e: any) {
    uni.hideLoading();
    if (e.message && e.message !== "chooseImage:fail cancel") {
      uni.showToast({ title: e.message, icon: "none" });
    }
  }
}

function saveNickname() {
  const name = editNickname.value.trim();
  if (!name) return;
  userStore.updateProfile({ nickname: name });
  showNicknameEdit.value = false;
}

function goCheckinShare() { uni.navigateTo({ url: "/pages/checkin-share/checkin-share" }); }
function goCheckinCalendar() { uni.navigateTo({ url: "/pages/checkin-calendar/checkin-calendar" }); }
function goAIChat() { uni.switchTab({ url: "/pages/chat/chat" }); }
function goLogin() { uni.navigateTo({ url: "/pages/login/login" }); }
function showAbout() { uni.showModal({ title: "拍立学", content: "拍照学习，快乐学英语\n版本 0.2.0", showCancel: false }); }
async function handleLogout() { const res = await uni.showModal({ title: "提示", content: "确定退出登录？" }); if (res.confirm) { await userStore.doLogout(); } }
</script>

<style lang="scss" scoped>
.profile { min-height: 100vh; background: $gradient-bg; }
.not-login { display: flex; flex-direction: column; align-items: center; padding-top: 160rpx; }
.user-header { display: flex; flex-direction: column; align-items: center; padding: 72rpx 0 44rpx; background: $bg-card; }
.avatar-wrap { position: relative; width: 128rpx; height: 128rpx; }
.avatar { width: 128rpx; height: 128rpx; border-radius: 50%; background: $bg-secondary; box-shadow: $shadow-sm; border: 4rpx solid $bg-card; }
.avatar-placeholder { display: flex; align-items: center; justify-content: center; font-size: 56rpx; background: $bg-secondary; }
.avatar-overlay { position: absolute; inset: 0; border-radius: 50%; background: rgba(0,0,0,0.2); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; font-size: 36rpx; }
.avatar-wrap:active .avatar-overlay { opacity: 1; }
.nickname { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; margin-top: $spacing-md; padding: 4rpx 8rpx; }
.edit-hint { font-size: $font-xs; color: $text-muted; margin-top: 4rpx; }
.level-badge { font-size: $font-sm; color: $primary; background: $bg-secondary; padding: 6rpx 20rpx; border-radius: $radius-pill; margin-top: $spacing-sm; }
.stats-row { display: flex; align-items: center; justify-content: space-around; background: $bg-card; padding: $spacing-xl; box-shadow: $shadow-sm; &.secondary { margin-top: 2rpx; } }
.stat-item { display: flex; flex-direction: column; align-items: center; .stat-num { font-size: $font-xxl; font-weight: $font-weight-extrabold; color: $primary; &.success { color: $success; } } .stat-label { font-size: $font-sm; color: $text-secondary; margin-top: 6rpx; } }
.stat-divider { width: 2rpx; height: 52rpx; background: $border-color; }
.menu-list { margin: $spacing-lg $spacing-xl 0; background: $bg-card; border-radius: $radius-md; box-shadow: $shadow-sm; overflow: hidden; }
.menu-item { display: flex; align-items: center; justify-content: space-between; padding: $spacing-xl $spacing-lg; border-bottom: 2rpx solid $border-light; &:active { background: $bg-primary; } .danger { color: $danger; } }
.menu-right { display: flex; align-items: center; gap: $spacing-sm; color: $text-muted; font-size: $font-base; }
.menu-badge { font-size: $font-sm; color: $primary; background: $bg-secondary; padding: 4rpx 14rpx; border-radius: $radius-pill; }

/* nickname edit modal */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 560rpx; background: #fff; border-radius: $radius-lg; padding: $spacing-xl; }
.modal-title { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; display: block; margin-bottom: $spacing-lg; }
.nickname-input { width: 100%; height: 76rpx; background: $bg-primary; border-radius: $radius-md; padding: 0 $spacing-lg; font-size: $font-base; box-sizing: border-box; }
.modal-actions { display: flex; gap: $spacing-base; margin-top: $spacing-lg; }
.btn-cancel { flex: 1; height: 72rpx; background: $bg-secondary; color: $text-secondary; border: none; border-radius: $radius-pill; font-size: $font-base; }
.btn-save { flex: 1; height: 72rpx; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; font-size: $font-base; &:disabled { opacity: 0.5; } }
</style>
