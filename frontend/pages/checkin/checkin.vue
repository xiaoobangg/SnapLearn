<template>
  <view class="checkin-page">
    <!-- ===== 选择词库 ===== -->
    <view class="bank-select" v-if="!selectedBankId">
      <view class="select-header">
        <text class="select-title">选择词库</text>
        <text class="select-desc">开始今天的打卡学习</text>
      </view>
      <view class="bank-list" v-if="banks.length > 0">
        <view class="bank-card" v-for="bank in banks" :key="bank.id" @click="selectBank(bank)">
          <view class="bc-icon" :class="bank.type">
            <text>{{ bank.type === 'system' ? '🤖' : (bank.type === 'preset' ? '📚' : '📖') }}</text>
          </view>
          <view class="bc-body">
            <text class="bc-name">{{ bank.name }}</text>
            <text class="bc-desc" v-if="bank.description">{{ bank.description }}</text>
            <text class="bc-type" v-if="bank.type === 'system'">系统自动</text>
          </view>
          <text class="bc-arrow">›</text>
        </view>
      </view>
      <view class="create-bank">
        <text class="cb-label">或创建新词库</text>
        <view class="cb-row">
          <input class="cb-input" v-model="newBankName" placeholder="词库名称" />
          <button class="cb-btn" @click="handleCreateBank" :disabled="!newBankName.trim()">创建</button>
        </view>
      </view>
    </view>

    <!-- ===== 打卡中 ===== -->
    <template v-else-if="!completed">
      <!-- 顶部状态栏 -->
      <view class="ci-topbar">
        <view class="ci-bank-info">
          <text class="ci-back" @click="selectedBankId=''">‹ 词库</text>
          <text class="ci-bank-name">{{ selectedBankName }}</text>
        </view>
        <view class="ci-progress">
          <view class="ci-progress-bar">
            <view class="ci-progress-fill" :style="{ width: (markedCount / totalWords * 100) + '%' }">
              <text class="ci-progress-smiley" v-if="totalWords > 0">😊</text>
            </view>
          </view>
          <text class="ci-progress-text">{{ markedCount }} / {{ totalWords }}</text>
        </view>
      </view>

      <!-- 新词 -->
      <view class="ci-section" v-if="newWords.length > 0">
        <view class="ci-section-head">
          <text class="ci-section-title">🆕 新词</text>
          <text class="ci-section-count">{{ markedNew }} / {{ newWords.length }}</text>
        </view>
        <swiper class="ci-swiper" :current="newIdx" @change="onNewSwipe" :style="{ height: swiperHeight + 'px' }">
          <swiper-item v-for="w in newWords" :key="w.pool_id">
            <DailyCheckinCard :word="w" type="new" :show-select="true"
              @mark="(pid, mark) => handleMark(pid, mark, 'new')"
              @toggle-select="(text) => toggleWordSelect(text)" />
          </swiper-item>
        </swiper>
        <view class="ci-dots" v-if="newWords.length > 1">
          <view v-for="(_, ni) in newWords" :key="ni" :class="['ci-dot', { active: ni === newIdx }]" />
        </view>
      </view>

      <!-- 复习词 -->
      <view class="ci-section" v-if="reviewWords.length > 0">
        <view class="ci-section-head">
          <text class="ci-section-title">🔄 复习</text>
          <text class="ci-section-count">{{ markedReview }} / {{ reviewWords.length }}</text>
        </view>
        <swiper class="ci-swiper" :current="reviewIdx" @change="onReviewSwipe" :style="{ height: swiperHeight + 'px' }">
          <swiper-item v-for="w in reviewWords" :key="w.pool_id">
            <DailyCheckinCard :word="w" type="review" @mark="(pid, mark) => handleMark(pid, mark, 'review')" />
          </swiper-item>
        </swiper>
        <view class="ci-dots" v-if="reviewWords.length > 1">
          <view v-for="(_, ri) in reviewWords" :key="ri" :class="['ci-dot', { active: ri === reviewIdx }]" />
        </view>
      </view>

      <!-- 空状态 -->
      <view class="ci-empty" v-if="totalWords === 0">
        <text class="ci-empty-icon">🎉</text>
        <text class="ci-empty-title">今天没有待打卡的单词</text>
        <text class="ci-empty-desc">去卡片组学习新单词，或等待复习词推送</text>
      </view>

      <!-- 完成按钮 -->
      <view class="ci-complete-bar" v-if="markedCount === totalWords && totalWords > 0">
        <button class="ci-complete-btn" @click="finishCheckin">✅ 完成打卡</button>
      </view>
    </template>

    <!-- ===== 打卡完成 ===== -->
    <view class="checkin-done" v-else>
      <view class="cd-card">
        <view class="cd-icon-wrap">
          <text class="cd-icon">🎉</text>
        </view>
        <text class="cd-title">打卡完成！</text>
        <text class="cd-date">{{ todayDate }}</text>
        <view class="cd-divider" />
        <view class="cd-stats">
          <view class="cd-stat known">
            <text class="cd-stat-num">{{ markCounts.known }}</text>
            <text class="cd-stat-label">😊 认识</text>
          </view>
          <view class="cd-stat fuzzy">
            <text class="cd-stat-num">{{ markCounts.fuzzy }}</text>
            <text class="cd-stat-label">🤔 模糊</text>
          </view>
          <view class="cd-stat unknown">
            <text class="cd-stat-num">{{ markCounts.unknown }}</text>
            <text class="cd-stat-label">😞 不认识</text>
          </view>
        </view>
        <view class="cd-actions">
          <button class="cd-btn continue" @click="continueCheckin">继续打卡</button>
          <button class="cd-btn share" @click="goShare">📸 分享打卡</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api"; import DailyCheckinCard from "@/components/DailyCheckinCard.vue";

const banks = ref<any[]>([]);
const selectedBankId = ref(""); const selectedBankName = ref(""); const newBankName = ref("");
const newWords = ref<any[]>([]); const reviewWords = ref<any[]>([]);
const swiperHeight = ref(500); const completed = ref(false);
const newIdx = ref(0); const reviewIdx = ref(0);
const markedNew = ref(0); const markedReview = ref(0);
const markCounts = reactive({ known: 0, fuzzy: 0, unknown: 0 });
const selectedWords = ref<string[]>([]);
const totalWords = computed(() => newWords.value.length + reviewWords.value.length);
const markedCount = computed(() => markedNew.value + markedReview.value);
const todayDate = computed(() => {
  const d = new Date();
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
});

onLoad(() => { const sys = uni.getSystemInfoSync(); swiperHeight.value = Math.max(460, (sys.windowHeight || 667) - 280); loadBanks(); });
async function loadBanks() { try { const res = await api.listWordBanks(); banks.value = (res as any).banks || []; } catch (_e) {} }
async function selectBank(bank: any) { selectedBankId.value = bank.id; selectedBankName.value = bank.name; await loadTodayWords(); }
async function handleCreateBank() { try { const res = await api.createWordBank(newBankName.value.trim()); const bank = (res as any).bank; if (bank) { selectedBankId.value = bank.id; selectedBankName.value = bank.name; await loadTodayWords(); } } catch (_e) { uni.showToast({ title: "创建失败", icon: "none" }); } }
async function loadTodayWords() { try { const data = await api.getTodayCheckin(selectedBankId.value); newWords.value = (data as any).new_words || []; reviewWords.value = (data as any).review_words || []; } catch (_e) { uni.showToast({ title: "加载失败", icon: "none" }); } }
function handleMark(poolId: string, mark: string, type: "new" | "review") {
  if (type === "new") {
    markedNew.value++;
    // Auto-advance to next new word, or to review section
    if (newIdx.value < newWords.value.length - 1) {
      setTimeout(() => { newIdx.value++; }, 300);
    }
  } else {
    markedReview.value++;
    if (reviewIdx.value < reviewWords.value.length - 1) {
      setTimeout(() => { reviewIdx.value++; }, 300);
    }
  }
  if (mark === "known") markCounts.known++; else if (mark === "fuzzy") markCounts.fuzzy++; else markCounts.unknown++;
  api.markCheckinWord(poolId, mark).catch(() => {});
  // Auto-complete when all words are marked
  if (markedCount.value >= totalWords.value && totalWords.value > 0) {
    setTimeout(() => finishCheckin(), 400);
  }
}
function toggleWordSelect(wordText: string) { const idx = selectedWords.value.indexOf(wordText); if (idx >= 0) selectedWords.value.splice(idx, 1); else selectedWords.value.push(wordText); }
function onNewSwipe(e: any) { newIdx.value = e.detail.current; }
function onReviewSwipe(e: any) { reviewIdx.value = e.detail.current; }

async function finishCheckin() {
  try {
    await api.completeCheckin(selectedBankId.value, {
      newCount: newWords.value.length,
      reviewCount: reviewWords.value.length,
      knownCount: markCounts.known,
      fuzzyCount: markCounts.fuzzy,
      unknownCount: markCounts.unknown,
    });
  } catch (_e) { /* ignore */ }
  completed.value = true;
}

function continueCheckin() { completed.value = false; }

function goShare() {
  uni.navigateTo({ url: "/pages/checkin-share/checkin-share" });
}
</script>

<style lang="scss" scoped>
.checkin-page { min-height: 100vh; background: $gradient-bg; }

// ===== 选择词库 =====
.bank-select { padding: 48rpx $spacing-xl; }
.select-header { margin-bottom: 32rpx; }
.select-title { display: block; font-size: 40rpx; font-weight: 800; color: $text-primary; }
.select-desc { display: block; font-size: $font-sm; color: $text-secondary; margin-top: 8rpx; }
.bank-list { display: flex; flex-direction: column; gap: 16rpx; margin-bottom: 40rpx; }
.bank-card { display: flex; align-items: center; background: $bg-card; border-radius: $radius-md; padding: 28rpx; box-shadow: $shadow-sm; gap: 20rpx;
  &:active { transform: scale(0.98); background: $bg-secondary; } }
.bc-icon { width: 88rpx; height: 88rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 40rpx; flex-shrink: 0;
  &.system { background: #EEF2FF; }
  &.preset { background: #D1FAE5; }
  &.user { background: #FEF3C7; } }
.bc-body { flex: 1; }
.bc-name { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; }
.bc-desc { display: block; font-size: $font-sm; color: $text-secondary; margin-top: 4rpx; }
.bc-type { display: inline-block; font-size: 20rpx; color: $primary; background: #EEF2FF; padding: 4rpx 12rpx; border-radius: $radius-pill; margin-top: 8rpx; }
.bc-arrow { font-size: 36rpx; color: $text-muted; }
.create-bank { background: $bg-card; border-radius: $radius-md; padding: 32rpx; box-shadow: $shadow-sm; }
.cb-label { font-size: $font-base; color: $text-secondary; display: block; margin-bottom: 16rpx; }
.cb-row { display: flex; gap: 16rpx; }
.cb-input { flex: 1; height: 80rpx; background: $bg-primary; border-radius: $radius-pill; padding: 0 28rpx; font-size: $font-base; }
.cb-btn { background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 20rpx 40rpx; font-size: $font-base; font-weight: $font-weight-semibold; &[disabled] { opacity: 0.4; } }

// ===== 打卡中 =====
.ci-topbar { background: $bg-card; padding: 16rpx $spacing-xl 20rpx; border-bottom: 2rpx solid $border-light; }
.ci-bank-info { display: flex; align-items: center; gap: 16rpx; margin-bottom: 16rpx; }
.ci-back { font-size: $font-base; color: $primary; }
.ci-bank-name { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; }
.ci-progress { display: flex; align-items: center; gap: 16rpx; }
.ci-progress-bar { flex: 1; height: 24rpx; background: #E5E7EB; border-radius: 12rpx; }
.ci-progress-fill { height: 100%; background: linear-gradient(90deg, #10B981, #34D399); border-radius: 12rpx; transition: width 0.3s; min-width: 48rpx; display: flex; align-items: center; justify-content: flex-end; }
.ci-progress-smiley { font-size: 44rpx; margin-left: auto; margin-right: -12rpx; transform: translateY(-4rpx); }
.ci-progress-text { font-size: $font-base; color: $text-secondary; flex-shrink: 0; font-weight: $font-weight-semibold; }
.ci-section { padding: 0 $spacing-xl; margin-top: 24rpx; }
.ci-section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.ci-section-title { font-size: $font-base; font-weight: $font-weight-bold; color: $text-primary; }
.ci-section-count { font-size: $font-sm; color: $text-secondary; }
.ci-swiper { width: 100%; }
.ci-dots { display: flex; justify-content: center; gap: 10rpx; margin-top: 20rpx; }
.ci-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #D1D5DB; transition: all 0.2s;
  &.active { background: $primary; width: 28rpx; border-radius: 6rpx; } }

.ci-empty { text-align: center; padding: 120rpx 0; }
.ci-empty-icon { font-size: 72rpx; display: block; margin-bottom: 20rpx; }
.ci-empty-title { font-size: $font-lg; color: $text-primary; font-weight: $font-weight-bold; display: block; }
.ci-empty-desc { font-size: $font-sm; color: $text-secondary; display: block; margin-top: 8rpx; }

.ci-complete-bar { padding: 24rpx $spacing-xl 48rpx; }
.ci-complete-btn { width: 100%; height: 96rpx; line-height: 96rpx; background: linear-gradient(135deg, #10B981, #34D399); color: #fff; border: none; border-radius: $radius-pill; font-size: $font-lg; font-weight: $font-weight-bold; box-shadow: 0 6rpx 20rpx rgba(16,185,129,0.3); }

// ===== 打卡完成 =====
.checkin-done { display: flex; justify-content: center; padding: 80rpx $spacing-xl; }
.cd-card { background: $bg-card; border-radius: $radius-lg; padding: 56rpx 48rpx; box-shadow: $shadow-sm; text-align: center; width: 100%; max-width: 600rpx; }
.cd-icon-wrap { width: 120rpx; height: 120rpx; border-radius: 50%; background: linear-gradient(135deg, #10B981, #34D399); display: flex; align-items: center; justify-content: center; margin: 0 auto 24rpx; box-shadow: 0 6rpx 24rpx rgba(16,185,129,0.3); }
.cd-icon { font-size: 56rpx; animation: bounce 1s ease infinite; }
@keyframes bounce { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-10rpx); } }
.cd-title { font-size: 40rpx; font-weight: 800; color: $text-primary; display: block; }
.cd-date { font-size: $font-sm; color: $text-secondary; display: block; margin-top: 8rpx; }
.cd-divider { height: 2rpx; background: $border-light; margin: 36rpx 0; }
.cd-stats { display: flex; justify-content: center; gap: 40rpx; }
.cd-stat { text-align: center; }
.cd-stat-num { font-size: 48rpx; font-weight: 800; display: block; }
.cd-stat-label { font-size: $font-sm; color: $text-secondary; display: block; margin-top: 4rpx; }
.cd-stat.known .cd-stat-num { color: #10B981; }
.cd-stat.fuzzy .cd-stat-num { color: #F59E0B; }
.cd-stat.unknown .cd-stat-num { color: #EF4444; }
.cd-actions { display: flex; gap: 20rpx; margin-top: 40rpx; }
.cd-btn { flex: 1; height: 80rpx; line-height: 80rpx; border-radius: $radius-pill; font-size: $font-base; font-weight: $font-weight-semibold; border: none; }
.cd-btn.continue { background: $bg-secondary; color: $text-primary; }
.cd-btn.share { background: $gradient-primary; color: #fff; box-shadow: $shadow-sm; }
</style>
