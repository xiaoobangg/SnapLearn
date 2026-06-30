<template>
  <view class="checkin-page">
    <view class="bank-select" v-if="!selectedBankId">
      <view class="section-header"><text class="section-title">&#x1F4C5; 选择词库</text><text class="section-desc">选择一个词库开始今天的打卡</text></view>
      <view class="bank-list" v-if="banks.length > 0">
        <view class="bank-card" v-for="bank in banks" :key="bank.id" @click="selectBank(bank)">
          <view class="bank-left"><text>{{ bank.type === 'preset' ? '\uD83D\uDCDA' : '\uD83D\uDCD6' }}</text></view>
          <view class="bank-body"><text class="bank-name">{{ bank.name }}</text><text class="bank-desc" v-if="bank.description">{{ bank.description }}</text></view>
          <text>&#x27A1;</text>
        </view>
      </view>
      <view class="create-bank">
        <view class="divider"><text>&#x1F4CC; 或创建新词库</text></view>
        <input class="bank-input" v-model="newBankName" placeholder="输入新词库名称" />
        <button class="btn-create" @click="handleCreateBank" :disabled="!newBankName.trim()">&#x2728; 创建词库</button>
      </view>
      <view class="empty-banks" v-if="banks.length === 0 && !newBankName">
        <text class="empty-icon">&#x1F4ED;</text>
        <text class="empty-text">还没有词库</text>
        <text class="empty-hint">创建一个词库开始打卡吧</text>
      </view>
    </view>
    <template v-else-if="!completed">
      <view class="checkin-header">
        <text class="checkin-bank-name">&#x1F4C5; {{ selectedBankName }}</text>
        <view class="checkin-progress"><text>{{ markedCount }} / {{ totalWords }}</text></view>
      </view>
      <view class="word-section" v-if="newWords.length > 0">
        <view class="ws-header"><text>&#x1F195; 新词</text><text>{{ markedNew }} / {{ newWords.length }}</text></view>
        <swiper class="word-swiper" :current="newIdx" @change="onNewSwipe" :style="{ height: swiperHeight + 'px' }">
          <swiper-item v-for="w in newWords" :key="w.pool_id"><DailyCheckinCard :word="w" type="new" :show-select="true" @mark="(pid, mark) => handleMark(pid, mark, 'new')" @toggle-select="(text) => toggleWordSelect(text)" /></swiper-item>
        </swiper>
        <view class="swipe-nav" v-if="newWords.length > 1">
          <view class="swipe-dot" v-for="(_, ni) in newWords" :key="'nd-'+ni" :class="{ active: ni === newIdx }" />
        </view>
      </view>
    </template>
    <view class="checkin-done" v-else>
      <view class="done-icon-wrap"><text>&#x1F389;</text></view>
      <text class="done-title">打卡完成！</text>
      <view class="done-stats">
        <view class="done-stat"><text class="ds-num known">{{ markCounts.known }}</text><text>&#x1F60A; 认识</text></view>
        <view class="done-stat"><text class="ds-num fuzzy">{{ markCounts.fuzzy }}</text><text>&#x1F914; 模糊</text></view>
        <view class="done-stat"><text class="ds-num unknown">{{ markCounts.unknown }}</text><text>&#x1F615; 不认识</text></view>
      </view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed, reactive } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api"; import DailyCheckinCard from "@/components/DailyCheckinCard.vue";
const banks = ref<any[]>([]); const selectedBankId = ref(""); const selectedBankName = ref(""); const newBankName = ref("");
const newWords = ref<any[]>([]); const reviewWords = ref<any[]>([]); const swiperHeight = ref(500); const completed = ref(false);
const newIdx = ref(0); const reviewIdx = ref(0);
const markedNew = ref(0); const markedReview = ref(0);
const markCounts = reactive({ known: 0, fuzzy: 0, unknown: 0 });
const totalWords = computed(() => newWords.value.length + reviewWords.value.length);
const markedCount = computed(() => markedNew.value + markedReview.value);
onLoad(() => { const sys = uni.getSystemInfoSync(); swiperHeight.value = Math.max(500, (sys.windowHeight || 667) - 260); loadBanks(); });
async function loadBanks() { try { const res = await api.listWordBanks(); banks.value = (res as any).banks || []; } catch (_e) {} }
async function selectBank(bank: any) { selectedBankId.value = bank.id; selectedBankName.value = bank.name; await loadTodayWords(); }
async function handleCreateBank() { try { const res = await api.createWordBank(newBankName.value.trim()); const bank = (res as any).bank; if (bank) { selectedBankId.value = bank.id; selectedBankName.value = bank.name; await loadTodayWords(); } } catch (_e) { uni.showToast({ title: "创建失败", icon: "none" }); } }
async function loadTodayWords() { try { const data = await api.getTodayCheckin(selectedBankId.value); newWords.value = (data as any).new_words || []; reviewWords.value = (data as any).review_words || []; } catch (_e) { uni.showToast({ title: "加载失败", icon: "none" }); } }
function handleMark(poolId: string, mark: string, type: "new" | "review") { if (type === "new") markedNew.value++; else markedReview.value++; if (mark === "known") markCounts.known++; else if (mark === "fuzzy") markCounts.fuzzy++; else markCounts.unknown++; api.markCheckinWord(poolId, mark).catch(()=>{}); }
function toggleWordSelect(wordText: string) { const idx = selectedWords.value.indexOf(wordText); if (idx >= 0) selectedWords.value.splice(idx, 1); else selectedWords.value.push(wordText); }
function onNewSwipe(e: any) { newIdx.value = e.detail.current; }
function onReviewSwipe(e: any) { reviewIdx.value = e.detail.current; }
</script>
<style lang="scss" scoped>
.checkin-page { min-height: 100vh; background: $gradient-bg; }
.bank-select { padding: $spacing-xxl $spacing-xl; }
.bank-card { display: flex; align-items: center; background: $bg-card; border-radius: $radius-md; padding: $spacing-lg; box-shadow: $shadow-sm; border: 2rpx solid $border-light; &:active { transform: scale(0.98); } }
.create-bank { background: $bg-card; border-radius: $radius-md; padding: $spacing-xl; box-shadow: $shadow-sm; .bank-input { background: $bg-primary; border-radius: $radius-pill; padding: 20rpx 28rpx; font-size: $font-base; height: 80rpx; margin-bottom: $spacing-md; } .btn-create { background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 22rpx 0; font-size: $font-base; font-weight: $font-weight-semibold; box-shadow: $shadow-sm; &[disabled] { opacity: 0.4; } } }
.checkin-header { display: flex; align-items: center; justify-content: space-between; padding: $spacing-lg $spacing-xl; background: $bg-card; }
.word-section { padding: $spacing-lg $spacing-xl; }
.checkin-done { display: flex; flex-direction: column; align-items: center; padding: 80rpx $spacing-xl; }
.done-icon-wrap { width: 140rpx; height: 140rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; box-shadow: $shadow-sm; margin-bottom: $spacing-lg; font-size: 64rpx; }
.done-title { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; }
.done-stats { display: flex; gap: $spacing-lg; margin-top: $spacing-xxl; .done-stat { display: flex; flex-direction: column; align-items: center; background: $bg-card; border-radius: $radius-md; padding: $spacing-lg $spacing-xl; box-shadow: $shadow-sm; .ds-num { font-size: $font-xxl; font-weight: $font-weight-bold; } &.known .ds-num { color: $success; } &.fuzzy .ds-num { color: $warning; } &.unknown .ds-num { color: $danger; } } }
</style>
