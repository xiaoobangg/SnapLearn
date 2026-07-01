<template>
  <view class="home">
    <view class="hero">
      <view class="hero-card">
        <view class="hero-chars">
          <text class="char-icon">&#x1F43C;</text>
          <text class="char-icon char-2">&#x1F430;</text>
        </view>
        <text class="greeting">Hi, &#x1F44B; 今天学点什么？</text>
        <text class="subtitle">拍照识别，快乐学英语</text>
        <view class="hero-dots">
          <view class="hd-dot"></view>
          <view class="hd-dot"></view>
          <view class="hd-dot"></view>
        </view>
      </view>
    </view>

    <view class="actions">
      <view class="action-card primary" @click="showTaskSheet = true" hover-class="card-hover">
        <view class="primary-icon">
          <text class="icon-emoji">&#x1F4DD;</text>
        </view>
        <view class="primary-text">
          <text class="action-title">创建学习任务</text>
          <text class="action-desc">拍照 / 上传图片 / 自己添加单词</text>
        </view>
        <text class="primary-arrow">&#x27A1;</text>
      </view>

      <view class="action-row">
        <view class="action-card half" @click="goCheckin" hover-class="card-hover">
          <view class="half-icon checkin-icon">
            <text class="icon-emoji">&#x1F4C5;</text>
          </view>
          <text class="half-title">每日打卡</text>
          <text class="half-desc">坚持就是胜利</text>
          <view class="badge" v-if="checkinStats.consecutive_days > 0">{{ checkinStats.consecutive_days }}天</view>
        </view>
        <view class="action-card half" @click="goNotebook" hover-class="card-hover">
          <view class="half-icon notebook-icon">
            <text class="icon-emoji">&#x1F4D6;</text>
          </view>
          <text class="half-title">学习本</text>
          <text class="half-desc">随时回顾</text>
        </view>
      </view>

      <view class="action-card chat-entry" @click="goAIChat" hover-class="card-hover">
        <view class="ce-icon-wrap">
          <text class="ce-icon">&#x1F916;</text>
        </view>
        <view class="ce-text">
          <text class="ce-title">AI 助手</text>
          <text class="ce-desc">问任何英语学习问题</text>
        </view>
        <text class="primary-arrow">&#x27A1;</text>
      </view>
    </view>

    <view class="task-sheet-overlay" v-if="showTaskSheet" @click="showTaskSheet = false">
      <view class="task-sheet" @click.stop>
        <view class="ts-close" @click="showTaskSheet = false"><text>&#x2715;</text></view>
        <view class="ts-handle"></view>
        <text class="ts-title">&#x2728; 创建学习任务</text>
        <view class="ts-option" @click="goCamera">
          <view class="tso-icon-wrap camera">
            <text class="tso-icon">&#x1F4F7;</text>
          </view>
          <view class="tso-body">
            <text class="tso-title">拍照</text>
            <text class="tso-desc">拍摄课本、文章，AI 自动提取单词</text>
          </view>
          <text class="tso-arrow">&#x203A;</text>
        </view>
        <view class="ts-option" @click="pickImage">
          <view class="tso-icon-wrap album">
            <text class="tso-icon">&#x1F5BC;</text>
          </view>
          <view class="tso-body">
            <text class="tso-title">上传图片</text>
            <text class="tso-desc">从相册选择截图或照片</text>
          </view>
          <text class="tso-arrow">&#x203A;</text>
        </view>
        <view class="ts-option" @click="showManualInput = true">
          <view class="tso-icon-wrap manual">
            <text class="tso-icon">&#x270F;&#xFE0F;</text>
          </view>
          <view class="tso-body">
            <text class="tso-title">自行添加单词</text>
            <text class="tso-desc">手动输入要学习的单词列表</text>
          </view>
          <text class="tso-arrow">&#x203A;</text>
        </view>
      </view>
    </view>

    <view class="manual-overlay" v-if="showManualInput" @click="showManualInput = false">
      <view class="manual-sheet" @click.stop>
        <text class="ms-title">&#x270F;&#xFE0F; 添加单词</text>
        <textarea class="ms-textarea" v-model="manualWords" placeholder="输入单词，每行一个或用逗号分隔" />
        <view class="ms-actions">
          <button class="ms-btn cancel" @click="showManualInput = false">取消</button>
          <button class="ms-btn confirm" @click="submitManualWords" :disabled="!manualWords.trim()">生成学习卡片</button>
        </view>
      </view>
    </view>

    <view class="section" v-if="groups.length > 0">
      <view class="section-header">
        <text class="section-title">&#x1F4DA; 我的卡片组</text>
        <text class="section-count">{{ groups.length }} 组</text>
      </view>
      <view class="group-list">
        <view class="card-swipe-wrap" v-for="group in groups" :key="group.id">
          <view class="card-del-bg" @tap.stop="deleteGroup(group)"><text>&#x1F5D1; 删除</text></view>
          <view class="group-card" :class="{ 'swiped': swipedId === group.id }" @click="handleCardClick(group)" @touchstart="onTouchStart($event, group.id)" @touchend="onTouchEnd($event, group.id)" hover-class="card-hover">
            <view class="group-left">
              <text class="group-icon">&#x1F4D3;</text>
            </view>
            <view class="group-body">
              <text class="group-title">{{ group.title || '未命名卡片组' }}</text>
              <text class="group-source" v-if="group.source_text">{{ group.source_text.substring(0, 50) }}{{ group.source_text.length > 50 ? '...' : '' }}</text>
            </view>
            <view class="group-right">
              <view class="group-status" :class="'gs-' + (group.group_status || 'pending')">{{ statusLabel(group.group_status) }}</view>
              <text class="group-count">{{ group.card_count || 0 }} 词</text>
              <text class="group-date">{{ formatDate(group.created_at) }}</text>
              <view class="group-test-btn" v-if="group.group_status === 'learn_done' || group.group_status === 'testing'" @tap.stop="goTest(group)">&#x1F4DD; 测验</view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else>
      <text class="empty-icon">&#x1F4F7;</text>
      <text class="empty-text">还没有学习记录</text>
      <text class="empty-hint">快去首页拍照开始学习吧！</text>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from "vue";
  import { onShow } from "@dcloudio/uni-app";
  import { api } from "@/api";

  const groups = ref<any[]>([]);
  const checkinStats = ref({ consecutive_days: 0, total_checkin_days: 0 });
  const swipedId = ref("");
  const touchStartX = ref(0);
  const showTaskSheet = ref(false);
  const showManualInput = ref(false);
  const manualWords = ref("");

  onShow(async () => {
    try { groups.value = await api.listCardGroups() || []; } catch (_e) { /* ignore */ }
    try { checkinStats.value = await api.getCheckinStats(); } catch (_e) { /* ignore */ }
  });

  function statusLabel(status : string) : string {
    const map: Record<string, string> = {
      pending: "待学习", learning: "学习中", learn_done: "已学完",
      testing: "测验中", test_done: "已通关"
    };
    return map[status] || status || "待学习";
  }

  function formatDate(dateStr : string) {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return "今天";
    if (days === 1) return "昨天";
    if (days < 7) return `${days}天前`;
    return `${d.getMonth() + 1}/${d.getDate()}`;
  }

  function onTouchStart(e : any, id : string) { touchStartX.value = e.touches[0].clientX; }
  function onTouchEnd(e : any, id : string) {
    const dx = e.changedTouches[0].clientX - touchStartX.value;
    if (dx < -60) { swipedId.value = id; } else { swipedId.value = ""; }
  }
  function handleCardClick(group : any) {
    if (swipedId.value) { swipedId.value = ""; return; }
    goCardDetail(group);
  }
  function goCamera() { showTaskSheet.value = false; uni.navigateTo({ url: "/pages/camera/camera" }); }
  function pickImage() {
    showTaskSheet.value = false;
    uni.chooseImage({ count: 1, sourceType: ["album"], success: (res) => { uni.navigateTo({ url: `/pages/select-words/select-words?imagePath=${encodeURIComponent(res.tempFilePaths[0])}` }); } });
  }
  function submitManualWords() {
    const text = manualWords.value.trim();
    if (!text) return;
    const words = text.split(/[\n,\uFF0C]+/).map(w => w.trim()).filter(Boolean);
    if (words.length === 0) return;
    showManualInput.value = false; showTaskSheet.value = false; manualWords.value = "";
    uni.navigateTo({ url: `/pages/select-words/select-words?manualWords=${encodeURIComponent(words.join(","))}` });
  }
  function goCheckin() { uni.navigateTo({ url: "/pages/checkin/checkin" }); }
  function goNotebook() { uni.switchTab({ url: "/pages/notebook/notebook" }); }
  function goAIChat() { uni.switchTab({ url: "/pages/chat/chat" }); }
  function goCardDetail(group : any) { uni.navigateTo({ url: `/pages/card-detail/card-detail?groupId=${group.id}` }); }
  function goTest(group : any) {
    const name = encodeURIComponent(group.title || "卡片组");
    uni.navigateTo({ url: `/pages/test/test?groupId=${group.id}&groupName=${name}` });
  }
  async function deleteGroup(group : any) {
    uni.showModal({
      title: "确认删除",
      content: `确定删除"${group.title || '未命名卡片组'}"？删除后不可恢复。`,
      success: async (res) => {
        if (res.confirm) {
          try { await api.deleteCardGroup(group.id); swipedId.value = ""; groups.value = groups.value.filter(g => g.id !== group.id); uni.showToast({ title: "已删除", icon: "success" }); }
          catch (_e) { uni.showToast({ title: "删除失败", icon: "none" }); }
        }
      },
    });
  }
</script>

<style lang="scss" scoped>
.home {
  padding: 0 $spacing-xl $spacing-xxl;
  min-height: 100vh;
  background: $gradient-bg;
}

.hero {
  padding: $spacing-xl 0 $spacing-xxl;
}

.hero-card {
  background: $gradient-sunset;
  border-radius: $radius-lg;
  padding: $spacing-huge $spacing-xxl $spacing-xxl;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(192, 132, 252, 0.2);

  &::after {
    content: "";
    position: absolute;
    top: -80rpx;
    right: -60rpx;
    width: 280rpx;
    height: 280rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
  }

  &::before {
    content: "";
    position: absolute;
    bottom: -40rpx;
    left: -30rpx;
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
  }
}

.hero-chars {
  display: flex;
  gap: 8rpx;
  margin-bottom: $spacing-md;
  position: relative;
  z-index: 1;

  .char-icon {
    font-size: 56rpx;
    animation: bounce 2s ease-in-out infinite;

    &.char-2 {
      font-size: 48rpx;
      animation-delay: 0.4s;
    }
  }
}

.hero-dots {
  display: flex;
  gap: 6rpx;
  margin-top: $spacing-base;
  position: relative;
  z-index: 1;

  .hd-dot {
    width: 8rpx;
    height: 8rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.3);

    &:nth-child(2) { opacity: 0.5; }
    &:nth-child(3) { opacity: 0.2; }
  }
}

.greeting {
  display: block;
  font-size: $font-xxl;
  font-weight: $font-weight-extrabold;
  color: #fff;
  margin-bottom: $spacing-sm;
  position: relative;
  z-index: 1;
}

.subtitle {
  font-size: $font-base;
  color: rgba(255, 255, 255, 0.75);
  position: relative;
  z-index: 1;
}

.actions {
  margin-bottom: $spacing-xl;
}

.action-card {
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-sm;
  border: 2rpx solid $border-light;
  transition: $transition-base;

  &.primary {
    display: flex;
    align-items: center;
    padding: $spacing-xl;
    margin-bottom: $spacing-base;

    .primary-icon {
      width: 104rpx;
      height: 104rpx;
      border-radius: $radius-md;
      background: $gradient-warm;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: $shadow-sm;
      .icon-emoji { font-size: 48rpx; }
    }

    .primary-text {
      flex: 1;
      margin-left: $spacing-lg;
      .action-title { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; display: block; margin-bottom: 4rpx; }
      .action-desc { font-size: $font-sm; color: $text-secondary; display: block; }
    }

    .primary-arrow { font-size: $font-base; color: $text-muted; }
  }

  &.half {
    flex: 1;
    padding: $spacing-lg $spacing-lg;
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;

    &:first-child { margin-right: 8rpx; }
    &:last-child { margin-left: 8rpx; }

    .half-icon {
      width: 88rpx;
      height: 88rpx;
      border-radius: $radius-md;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-sm;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
      .icon-emoji { font-size: 40rpx; }
    }

    .checkin-icon { background: $gradient-warm; }
    .notebook-icon { background: $gradient-secondary; }

    .half-title { font-size: $font-base; font-weight: $font-weight-bold; color: $text-primary; margin-bottom: 2rpx; }
    .half-desc { font-size: 22rpx; color: $text-muted; }
  }
}

.card-hover {
  transform: scale(0.97);
  box-shadow: $shadow-md;
}

.action-row { display: flex; }

.chat-entry {
  display: flex;
  align-items: center;
  margin-top: $spacing-base;
  padding: $spacing-md $spacing-lg;
  background: $bg-card;
  border-radius: $radius-md;
  border: 2rpx solid $border-light;
  box-shadow: $shadow-sm;

  .ce-icon-wrap {
    width: 72rpx;
    height: 72rpx;
    border-radius: $radius-sm;
    background: $gradient-mint;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: $spacing-md;
    .ce-icon { font-size: 36rpx; }
  }

  .ce-text {
    flex: 1;
    .ce-title { font-size: $font-base; font-weight: $font-weight-bold; color: $text-primary; display: block; }
    .ce-desc { font-size: 22rpx; color: $text-secondary; margin-top: 2rpx; }
  }

  .primary-arrow { font-size: $font-base; color: $text-muted; }
}

.badge {
  position: absolute;
  top: $spacing-sm;
  right: $spacing-sm;
  min-width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  border-radius: $radius-pill;
  background: $gradient-primary;
  color: #fff;
  font-size: 22rpx;
  font-weight: $font-weight-bold;
  padding: 0 $spacing-sm;
  box-shadow: $shadow-sm;
}

.section { margin-bottom: $spacing-lg; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-lg;
  .section-title { font-size: $font-xl; font-weight: $font-weight-bold; color: $text-primary; }
  .section-count { font-size: $font-sm; color: $text-secondary; background: $bg-secondary; padding: 6rpx 20rpx; border-radius: $radius-pill; }
}

.group-list { display: flex; flex-direction: column; gap: $spacing-sm; }

.card-swipe-wrap { position: relative; overflow: hidden; border-radius: $radius-md; }

.card-del-bg {
  position: absolute; right: 0; top: 0; bottom: 0; width: 120rpx;
  background: $gradient-primary; border-radius: 0 $radius-md $radius-md 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: $font-base; font-weight: $font-weight-semibold;
}

.group-card {
  position: relative; z-index: 1;
  display: flex; align-items: center;
  background: $bg-card; border-radius: $radius-md; padding: $spacing-lg;
  box-shadow: $shadow-sm; border: 2rpx solid $border-light;
  transition: transform 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  &.swiped { transform: translateX(-120rpx); }
}

.group-left {
  width: 80rpx; height: 80rpx; border-radius: $radius-sm;
  background: $bg-secondary; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin-right: $spacing-md; box-shadow: $shadow-sm;
  .group-icon { font-size: 36rpx; }
}

.group-body {
  flex: 1; overflow: hidden; margin-right: $spacing-sm;
  .group-title { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4rpx; }
  .group-source { font-size: $font-sm; color: $text-muted; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.group-right {
  flex-shrink: 0; text-align: right;
  .group-status { font-size: 20rpx; padding: 6rpx 14rpx; border-radius: $radius-pill; font-weight: $font-weight-semibold; margin-bottom: $spacing-xs; display: inline-block;
    &.gs-pending { background: $bg-secondary; color: $text-secondary; }
    &.gs-learning { background: #FFF3BF; color: #E67700; }
    &.gs-learn_done { background: #E0E7FF; color: #6366F1; }
    &.gs-testing { background: #FFF0E8; color: #F97316; }
    &.gs-test_done { background: #D1FAE5; color: #059669; }
  }
  .group-count { font-size: $font-sm; font-weight: $font-weight-bold; color: $primary; background: $bg-secondary; padding: 6rpx 16rpx; border-radius: $radius-pill; display: inline-block; margin-bottom: $spacing-xs; }
  .group-date { font-size: 22rpx; color: $text-muted; display: block; }
  .group-test-btn { font-size: 28rpx; color: #fff; font-weight: $font-weight-bold; margin-top: $spacing-sm; padding: 16rpx 36rpx; background: $gradient-primary; border-radius: $radius-pill; display: inline-block; box-shadow: 0 6rpx 16rpx rgba(255, 138, 155, 0.35); letter-spacing: 1rpx; }
}

.empty-state { display: flex; flex-direction: column; align-items: center; padding-top: 160rpx;
  .empty-icon { font-size: 80rpx; margin-bottom: $spacing-lg; opacity: 0.5; }
  .empty-text { font-size: $font-lg; color: $text-secondary; font-weight: $font-weight-semibold; margin-bottom: $spacing-xs; }
  .empty-hint { font-size: $font-base; color: $text-muted; }
}

.task-sheet-overlay { position: fixed; inset: 0; background: $bg-overlay; backdrop-filter: blur(8rpx); display: flex; align-items: flex-end; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease; }
.task-sheet { background: $bg-card; border-radius: $radius-xl $radius-xl 0 0; width: 100%; padding: $spacing-base $spacing-xl $spacing-lg; animation: slideUp 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.ts-close { position: absolute; top: $spacing-lg; right: $spacing-lg; width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; border-radius: 50%; background: $bg-secondary; font-size: $font-base; color: $text-secondary; z-index: 1; }
.ts-handle { width: 48rpx; height: 6rpx; border-radius: 3rpx; background: $border-color; margin: 0 auto $spacing-lg; }
.ts-title { font-size: $font-lg; font-weight: $font-weight-extrabold; color: $text-primary; display: block; margin-bottom: $spacing-lg; text-align: center; }
.ts-option { display: flex; align-items: center; padding: $spacing-lg $spacing-md; border-radius: $radius-md; margin-bottom: $spacing-sm; background: $bg-primary; transition: $transition-fast; &:active { background: $bg-secondary; transform: scale(0.98); }
  .tso-icon-wrap { width: 72rpx; height: 72rpx; border-radius: $radius-sm; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-right: $spacing-md; .tso-icon { font-size: 36rpx; }
    &.camera { background: $gradient-warm; } &.album { background: $gradient-mint; } &.manual { background: $warning; } }
  .tso-body { flex: 1; .tso-title { font-size: $font-base; font-weight: $font-weight-bold; color: $text-primary; display: block; margin-bottom: 4rpx; } .tso-desc { font-size: 22rpx; color: $text-secondary; line-height: 1.5; } }
  .tso-arrow { font-size: 36rpx; color: $text-muted; margin-left: $spacing-sm; } }

.manual-overlay { position: fixed; inset: 0; background: $bg-overlay; backdrop-filter: blur(8rpx); display: flex; align-items: flex-start; justify-content: center; padding-top: 24vh; z-index: 1100; }
.manual-sheet { background: $bg-card; border-radius: $radius-lg; padding: $spacing-xl $spacing-xl; width: 600rpx; box-shadow: $shadow-lg; animation: popIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  .ms-title { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; display: block; margin-bottom: $spacing-md; text-align: center; }
  .ms-textarea { width: 100%; height: 300rpx; background: $bg-primary; border-radius: $radius-md; padding: $spacing-md; font-size: $font-base; color: $text-primary; box-sizing: border-box; border: 2rpx solid $border-color; }
  .ms-actions { display: flex; gap: $spacing-base; margin-top: $spacing-lg; }
  .ms-btn { flex: 1; border-radius: $radius-pill; padding: 20rpx 0; font-size: $font-base; font-weight: $font-weight-semibold; border: none; text-align: center;
    &.cancel { background: $bg-secondary; color: $text-secondary; }
    &.confirm { background: $gradient-primary; color: #fff; box-shadow: $shadow-sm; &[disabled] { opacity: 0.4; } } } }
</style>
