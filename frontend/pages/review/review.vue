<template>
  <view class="review-page">
    <view class="top-bar"><text class="back-btn" @click="goBack">&#x2190; 返回</text><view class="progress-wrap" v-if="swiperRef"><view class="progress-bar"><view v-for="card in cards" :key="'pb-'+card.card_id" class="pb-seg" :class="swiperRef.segClass(card.card_id)" :style="{ width: (100/cards.length)+'%' }" /></view><text class="progress-text">{{ swiperRef.ratedCount }} / {{ cards.length }}</text></view><view class="placeholder" /></view>
    <FlashCardSwiper ref="swiperRef" :cards="cards" card-id-key="card_id" completion-title="&#x1F389; 今日复习完成！" :completion-desc="'\u5171\u590D\u4E60\u4E86 ' + cards.length + ' \u4E2A\u5355\u8BCD'" @back="goBack" />
  </view>
</template>
<script setup lang="ts">
import { ref } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api"; import type { ReviewCard } from "@/api/types"; import FlashCardSwiper from "@/components/FlashCardSwiper.vue";
const cards = ref<ReviewCard[]>([]); const swiperRef = ref<InstanceType<typeof FlashCardSwiper>>();
onLoad(async () => { try { const result = await api.getTodayReview(); cards.value = result.cards || []; } catch(_e) { uni.showToast({ title:"加载失败", icon:"none" }); } });
function goBack() { uni.switchTab({ url:"/pages/index/index" }); }
</script>
<style lang="scss" scoped>
.review-page { min-height: 100vh; background: $bg-primary; display: flex; flex-direction: column; }
.top-bar { display: flex; align-items: center; padding: $spacing-lg $spacing-xl $spacing-base; gap: $spacing-base; .back-btn { color: $primary; font-size: $font-base; background: $bg-card; padding: 12rpx 24rpx; border-radius: $radius-pill; box-shadow: $shadow-sm; } .progress-text { font-size: $font-sm; color: $text-secondary; flex-shrink: 0; min-width: 72rpx; } }
</style>
