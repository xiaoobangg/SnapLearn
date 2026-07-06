<template>
  <view class="feedback">
    <view class="header">
      <text class="header-icon">💬</text>
      <text class="header-title">意见反馈</text>
      <text class="header-desc">有任何问题或建议欢迎联系我们</text>
    </view>

    <!-- 我的反馈记录 -->
    <view class="section" v-if="userStore.isLoggedIn">
      <text class="section-title">📋 我的反馈</text>
      <view class="fb-scroll">
      <view class="fb-item" v-for="fb in myFeedbacks" :key="fb.id">
        <view class="fb-head">
          <text class="fb-time">{{ fb.created_at?.substring(0, 10) }}</text>
          <text class="fb-status" :class="fb.status">{{ fb.status === 'replied' ? '已回复' : '待处理' }}</text>
        </view>
        <text class="fb-content">{{ fb.content }}</text>
        <view class="fb-replies" v-if="fb.replies?.length">
          <view class="reply-item" v-for="(r, i) in fb.replies" :key="i">
            <text class="reply-label">客服回复：</text>
            <text>{{ r.content }}</text>
          </view>
        </view>
      </view>
      <view class="fb-empty" v-if="myFeedbacks.length === 0">
        <text>暂无反馈记录</text>
      </view>
      </view>
    </view>

    <!-- 联系客服 -->
    <view class="contact-card">
      <text class="contact-title">📱 联系客服</text>
      <view class="contact-row">
        <text class="contact-label">微信号</text>
        <text class="contact-value" selectable>xiaoobangg</text>
      </view>
      <view class="contact-tip">添加时请备注「拍立学」</view>
    </view>

    <!-- 提交反馈 -->
    <view class="feedback-form" v-if="userStore.isLoggedIn">
      <text class="form-title">📝 快速反馈</text>
      <textarea
        v-model="content"
        class="form-textarea"
        placeholder="请描述您遇到的问题或建议..."
        :maxlength="500"
        auto-height
      />
      <view class="form-footer">
        <text class="char-count">{{ content.length }}/500</text>
        <button class="btn-submit" :disabled="!content.trim() || submitting" @click="submitFeedback">
          {{ submitting ? '提交中...' : '提交反馈' }}
        </button>
      </view>
    </view>

    <view class="not-login" v-else>
      <text class="nl-text">登录后可查看反馈记录和提交反馈</text>
      <button class="btn-login" @click="goLogin">登录</button>
    </view>

    <text class="version">拍立学 v1.0.0</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { api } from "@/api";

const userStore = useUserStore();
const content = ref("");
const submitting = ref(false);
const myFeedbacks = ref<any[]>([]);

async function loadMyFeedbacks() {
  if (!userStore.isLoggedIn) return;
  try {
    const res = await api.getMyFeedbacks();
    myFeedbacks.value = res || [];
  } catch { /* */ }
}

async function submitFeedback() {
  if (!content.value.trim() || submitting.value) return;
  submitting.value = true;
  try {
    await api.submitFeedback({ content: content.value });
    uni.showToast({ title: "感谢反馈！", icon: "success" });
    content.value = "";
    loadMyFeedbacks();
  } catch {
    uni.showToast({ title: "提交失败，请直接联系客服", icon: "none" });
  }
  submitting.value = false;
}

function goLogin() {
  uni.navigateTo({ url: "/pages/login/login" });
}

onShow(() => { loadMyFeedbacks(); });
</script>

<style lang="scss" scoped>
.feedback {
  min-height: 100vh;
  background: $gradient-bg;
  padding: $spacing-lg $spacing-base;
}

.header {
  text-align: center;
  padding: $spacing-huge 0 $spacing-lg;
  .header-icon { font-size: 64rpx; display: block; margin-bottom: $spacing-base; }
  .header-title { font-size: 40rpx; font-weight: 700; color: $text-primary; display: block; }
  .header-desc { font-size: 26rpx; color: $text-secondary; margin-top: $spacing-sm; display: block; padding: 0 $spacing-lg; }
}

.section {
  margin-bottom: $spacing-lg;
  .section-title { font-size: 30rpx; font-weight: 600; color: $text-primary; display: block; margin-bottom: $spacing-base; }
}
.fb-scroll { max-height: 400rpx; overflow-y: auto; }

.fb-item {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-base $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-sm;
}
.fb-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-xs; }
.fb-time { font-size: 24rpx; color: $text-muted; }
.fb-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: $radius-sm;
  &.pending { background: #FFF3E0; color: #E65100; }
  &.replied { background: #E8F5E9; color: #2E7D32; }
}
.fb-content { font-size: 28rpx; color: $text-primary; line-height: 1.6; }

.fb-empty { text-align: center; padding: $spacing-lg; font-size: 26rpx; color: $text-muted; }
.fb-replies { margin-top: $spacing-sm; padding-top: $spacing-sm; border-top: 1px solid #f0f0f0; }
.reply-item { font-size: 26rpx; color: $text-secondary; padding: 4rpx 0; .reply-label { color: $primary; font-weight: 600; } }

.contact-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-sm;
  .contact-title { font-size: 32rpx; font-weight: 600; color: $text-primary; display: block; margin-bottom: $spacing-base; }
}

.contact-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: $spacing-base 0;
  .contact-label { font-size: 28rpx; color: $text-secondary; }
  .contact-value { font-size: 32rpx; color: $primary; font-weight: 600; }
}

.contact-tip { font-size: 24rpx; color: $text-muted; margin-top: $spacing-sm; }

.feedback-form {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg;
  box-shadow: $shadow-sm;
  .form-title { font-size: 32rpx; font-weight: 600; color: $text-primary; display: block; margin-bottom: $spacing-base; }
}

.form-textarea {
  width: 100%;
  min-height: 200rpx;
  background: $bg-primary;
  border-radius: $radius-md;
  padding: $spacing-base $spacing-lg;
  font-size: 28rpx;
  color: $text-primary;
  box-sizing: border-box;
}

.form-footer {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: $spacing-base;
  .char-count { font-size: 24rpx; color: $text-muted; }
}

.btn-submit {
  background: $gradient-primary;
  color: #fff;
  border: none;
  border-radius: $radius-pill;
  padding: $spacing-sm $spacing-lg;
  font-size: 28rpx;
  &[disabled] { opacity: 0.5; }
}

.not-login { text-align: center; padding: $spacing-huge 0; }
.nl-text { font-size: 28rpx; color: $text-secondary; display: block; margin-bottom: $spacing-base; }
.btn-login { background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: $spacing-sm $spacing-lg; font-size: 28rpx; }

.version { text-align: center; font-size: 24rpx; color: $text-muted; display: block; padding: $spacing-huge 0; }
</style>
