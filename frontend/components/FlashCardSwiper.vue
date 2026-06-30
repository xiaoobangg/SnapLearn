<template>
  <view class="fcs-root">
    <view class="done" v-if="finished">
      <view class="done-icon-wrap"><text class="done-title-icon">&#x1F389;</text></view>
      <text class="done-title">{{ completionTitle }}</text>
      <text class="done-desc">{{ completionDesc }}</text>
      <view class="done-summary">
        <view class="ds-item"><view class="ds-dot mastered" /><text class="ds-label">已掌握</text><text class="ds-num">{{ summary.easy }}</text></view>
        <view class="ds-item"><view class="ds-dot normal" /><text class="ds-label">一般</text><text class="ds-num">{{ summary.medium }}</text></view>
        <view class="ds-item"><view class="ds-dot hard" /><text class="ds-label">需加强</text><text class="ds-num">{{ summary.hard }}</text></view>
      </view>
      <button class="btn-back-home" @click="$emit('back')">&#x1F3E0; 返回首页</button>
    </view>
    <template v-else-if="cards.length > 0">
      <swiper class="card-swiper" :current="currentIndex" @change="onSwipeChange" :style="{ height: swiperHeight + 'px' }">
        <swiper-item v-for="(card, i) in cards" :key="cardId(card)">
          <view class="card-wrap">
            <view class="flip-card" :class="{ flipped: flipped[i] }" @click="flipCard(i)">
              <view class="card-face front">
                <view class="word-row"><text class="card-word">{{ card.word }}</text><text class="speak-btn" @click.stop="speakWord(card.word)">&#x1F50A;</text></view>
                <text class="card-pos" v-if="card.pos">{{ card.pos }}</text>
                <text class="card-hint">点击翻转查看释义</text>
              </view>
              <view class="card-face back">
                <text class="back-word">{{ card.word }}</text>
                <view class="back-section" v-if="card.general_meaning"><text class="back-label">&#x1F4D6; 释义</text><text>{{ card.general_meaning }}</text></view>
                <view class="back-section" v-if="card.example_sentence"><text class="back-label">&#x1F4DD; 例句</text><text class="back-value example">{{ card.example_sentence }}</text></view>
                <view class="back-section" v-if="card.memory_tip"><text class="back-label">&#x1F9E0; 记忆技巧</text><text class="back-value tip">{{ card.memory_tip }}</text></view>
              </view>
            </view>
            <view class="ratings" v-if="flipped[i]">
              <view class="rating-btn forgot" @click="rate(i, 1)"><text>&#x1F615; 忘了</text></view>
              <view class="rating-btn hard" @click="rate(i, 3)"><text>&#x1F914; 困难</text></view>
              <view class="rating-btn good" @click="rate(i, 4)"><text>&#x1F60A; 一般</text></view>
              <view class="rating-btn easy" @click="rate(i, 5)"><text>&#x1F929; 简单</text></view>
            </view>
          </view>
        </swiper-item>
      </swiper>
    </template>
  </view>
</template>
<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue"; import { api } from "@/api"; import { getApiBaseUrl, getServerBaseUrl } from "@/config";
export interface CardLike { word: string; general_meaning?: string; example_sentence?: string; memory_tip?: string; pronunciation?: string; pos?: string; [key: string]: any; }
const props = withDefaults(defineProps<{ cards: CardLike[]; cardIdKey?: string; completionTitle?: string; completionDesc?: string; }>(), { cardIdKey: "id", completionTitle: "复习完成！", completionDesc: "" });
defineEmits<{ back: []; rated: [cardId: string, quality: number]; }>();
const currentIndex = ref(0); const flipped = ref<Record<number, boolean>>({}); const finished = ref(false); const swiperHeight = ref(600);
const ratings = reactive<Record<string, number>>({});
const ratedCount = computed(() => Object.keys(ratings).length);
const summary = computed(() => { let h=0, m=0, e=0; for (const q of Object.values(ratings)) { if (q<=1) h++; else if (q<=3) m++; else e++; } return { hard:h, medium:m, easy:e }; });
function cardId(card: CardLike): string { return card[props.cardIdKey] || ""; }
onMounted(() => { const sys = uni.getSystemInfoSync(); swiperHeight.value = (sys.windowHeight || 667) - 140; });
function flipCard(i: number) { flipped.value = { ...flipped.value, [i]: !flipped.value[i] }; }
function onSwipeChange(e: any) { currentIndex.value = e.detail.current; }
async function rate(i: number, quality: number) { const card = props.cards[i]; if (!card) return; const id = cardId(card); ratings[id] = quality; try { await api.submitReview(id, quality); } catch (_e) {} if (ratedCount.value >= props.cards.length) { finished.value = true; } else { const next = findNextUnrated(i); if (next >= 0) { currentIndex.value = next; flipped.value = {}; } } }
function findNextUnrated(from: number): number { for (let j=from+1; j<props.cards.length; j++) { if (!ratings[cardId(props.cards[j])]) return j; } for (let j=0; j<from; j++) { if (!ratings[cardId(props.cards[j])]) return j; } return -1; }
function segClass(cardId: string) { const q = ratings[cardId]; if (q===undefined) return ""; if (q<=1) return "seg-forgot"; if (q<=3) return "seg-hard"; if (q<=4) return "seg-good"; return "seg-easy"; }
function speakWord(word: string) { const t = uni.getStorageSync("access_token"); const c = props.cards[currentIndex.value]?.id||""; uni.request({ url: `${getApiBaseUrl()}/tts?text=${encodeURIComponent(word)}&cardId=${c}&type=word`, header: { Authorization: `Bearer ${t}` }, success: (r:any) => { if (r.data?.audio_url) { const a = uni.createInnerAudioContext(); a.src = `${getServerBaseUrl()}/${r.data.audio_url}`; a.play(); } } }); }
defineExpose({ ratings, ratedCount, segClass });
</script>
<style lang="scss" scoped>
.fcs-root { flex: 1; display: flex; flex-direction: column; }
.done { display: flex; flex-direction: column; align-items: center; padding-top: 80rpx; .done-icon-wrap { width: 140rpx; height: 140rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; margin-bottom: $spacing-lg; font-size: 64rpx; } .done-title { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; margin-top: $spacing-lg; } .done-desc { font-size: $font-base; color: $text-secondary; } }
.done-summary { display: flex; gap: $spacing-xxl; margin-top: $spacing-xxl; background: $bg-card; border-radius: $radius-md; padding: $spacing-xl $spacing-huge; box-shadow: $shadow-sm; }
.ds-dot { width: 24rpx; height: 24rpx; border-radius: 50%; &.mastered { background: $success; } &.normal { background: $info; } &.hard { background: $danger; } }
.btn-back-home { margin-top: 56rpx; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 24rpx 80rpx; font-size: $font-lg; font-weight: $font-weight-semibold; box-shadow: $shadow-sm; }
.flip-card { width: 650rpx; height: 780rpx; position: relative; transition: transform 0.4s ease; &.flipped { .front { transform: rotateY(180deg); } .back { transform: rotateY(0deg); } } }
.card-face { position: absolute; inset: 0; backface-visibility: hidden; border-radius: $radius-lg; padding: $spacing-huge; transition: transform 0.4s ease; }
.card-face.front { background: $gradient-sunset; display: flex; flex-direction: column; align-items: center; justify-content: center; box-shadow: $shadow-lg; }
.card-face.back { background: $bg-card; transform: rotateY(180deg); overflow-y: auto; box-shadow: $shadow-lg; }
.card-word { font-size: 68rpx; font-weight: $font-weight-extrabold; color: #fff; text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1); }
.card-pos { font-size: $font-base; color: rgba(255,255,255,0.75); background: rgba(255,255,255,0.2); backdrop-filter: blur(4rpx); padding: 8rpx 28rpx; border-radius: $radius-pill; }
.card-hint { position: absolute; bottom: 60rpx; font-size: $font-sm; color: rgba(255,255,255,0.5); }
.back-word { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; padding-bottom: $spacing-md; border-bottom: 2rpx solid $border-light; margin-bottom: $spacing-lg; }
.back-section { margin-bottom: $spacing-lg; .back-label { font-size: 22rpx; color: $text-muted; display: block; margin-bottom: $spacing-xs; } .back-value { font-size: $font-base; line-height: 1.7; &.example { font-style: italic; color: $primary; } &.tip { background: #FEF3C7; padding: 18rpx; border-radius: $radius-sm; display: block; } } }
.ratings { display: flex; justify-content: space-between; width: 650rpx; margin-top: $spacing-lg; gap: $spacing-sm; }
.rating-btn { flex: 1; height: 82rpx; line-height: 82rpx; text-align: center; border-radius: $radius-pill; font-size: $font-base; font-weight: $font-weight-semibold; transition: $transition-fast; &:active { transform: scale(0.94); } &.forgot { background: #FFE4E6; color: #E11D48; } &.hard { background: #FEF3C7; color: #D97706; } &.good { background: #D1FAE5; color: #059669; } &.easy { background: #DBEAFE; color: #2563EB; } }
</style>
