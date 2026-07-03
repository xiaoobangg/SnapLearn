<template>
  <view class="dcc-root">
    <view class="dcc-card" :class="{ 'is-new': type === 'new' }">
      <view class="dcc-type-tag">{{ type === 'new' ? '🆕 新词' : '🔄 复习' }}</view>
      <view class="dcc-word-row"><text class="dcc-word">{{ word.word_text }}</text><text class="dcc-speak" @click="speak(word.word_text, 'word')">🔊</text></view>
      <text class="dcc-pos" v-if="word.pos">{{ word.pos }}</text>
      <text class="dcc-pron" v-if="word.pronunciation">{{ word.pronunciation }}</text>

      <!-- 知识点列表（展开时显示，限高可滚动） -->
      <scroll-view scroll-y class="dcc-kp-scroll" v-if="showAllKps">
        <view class="dcc-kp-item" v-for="kp in knowledgePoints" :key="kp.type" @click="speak(kp.content, kp.type)">
          <view class="dcc-kp-header">
            <view class="dcc-kp-badge" :class="kp.type">{{ kp.label }}</view>
            <text class="dcc-kp-speak">🔊</text>
          </view>
          <text class="dcc-kp-content">{{ kp.content }}</text>
        </view>
      </scroll-view>

      <!-- 简化释义 / 收起 -->
      <view class="dcc-meaning-toggle" @click="showAllKps = !showAllKps">
        <text class="dcc-meaning-text" v-if="!showAllKps">{{ word.general_meaning || '点击查看完整释义' }}</text>
        <text class="dcc-meaning-text" v-else>收起 ▲</text>
        <text class="dcc-meaning-hint" v-if="!showAllKps">点击查看更多知识点</text>
      </view>
    </view>

    <!-- 操作按钮始终可见 -->
    <view class="dcc-actions" v-if="!marked">
      <view class="dcc-btn unknown" @click="handleMark('unknown')"><text>😞</text><text>不认识</text></view>
      <view class="dcc-btn fuzzy" @click="handleMark('fuzzy')"><text>🤔</text><text>模糊</text></view>
      <view class="dcc-btn known" @click="handleMark('known')"><text>😊</text><text>认识</text></view>
    </view>
    <view class="dcc-marked" v-else :class="'mark-' + markValue"><text>{{ markIcon }}</text><text>{{ markLabel }}</text></view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed } from "vue"; import { getApiBaseUrl, getServerBaseUrl } from "@/config";
export interface CheckinWord { pool_id: string; word_id?: string; word_text: string; pronunciation?: string; pos?: string; general_meaning?: string; extended_meaning?: string; example_sentence?: string; memory_tip?: string; }
const props = defineProps<{ word: CheckinWord; type: 'new' | 'review'; showSelect?: boolean; }>();
const emit = defineEmits<{ mark: [poolId: string, mark: string]; toggleSelect: [wordText: string]; }>();
const showAllKps = ref(false); const marked = ref(false); const markValue = ref("");
const markIcon = computed(() => ({ known: "😊", fuzzy: "🤔", unknown: "😞" }[markValue.value] || ""));
const markLabel = computed(() => ({ known: "已标记：认识", fuzzy: "已标记：模糊", unknown: "已标记：不认识" }[markValue.value] || ""));
const knowledgePoints = computed(() => {
  const pts: { type: string; label: string; content: string }[] = [];
  if (props.word.pronunciation) pts.push({ type: 'pronunciation', label: '音标', content: props.word.pronunciation! });
  if (props.word.pos) pts.push({ type: 'pos', label: '词性', content: props.word.pos! });
  if (props.word.general_meaning) pts.push({ type: 'general_meaning', label: '释义', content: props.word.general_meaning! });
  if (props.word.extended_meaning) pts.push({ type: 'extended_meaning', label: '延伸义', content: props.word.extended_meaning! });
  if (props.word.example_sentence) pts.push({ type: 'example_sentence', label: '例句', content: props.word.example_sentence! });
  if (props.word.memory_tip) pts.push({ type: 'memory_tip', label: '记忆技巧', content: props.word.memory_tip! });
  return pts;
});
function handleMark(mark: string) { markValue.value = mark; marked.value = true; emit("mark", props.word.pool_id, mark); }
function speak(text: string, audioType: string) {
  if (!text) return;
  const t = uni.getStorageSync("access_token");
  const wid = props.word.word_id || "";
  uni.request({ url: `${getApiBaseUrl()}/tts?text=${encodeURIComponent(text)}&type=${audioType}&wordId=${wid}`, header: { Authorization: `Bearer ${t}` }, success: (res: any) => { if (res.data?.audio_url) { const a = uni.createInnerAudioContext(); a.src = getServerBaseUrl() + "/" + res.data.audio_url; a.play(); } } });
}
</script>
<style lang="scss" scoped>
.dcc-root { display: flex; flex-direction: column; align-items: center; padding: $spacing-lg 0; }
.dcc-card { width: 620rpx; background: $bg-card; border-radius: $radius-lg; padding: 48rpx 40rpx; box-shadow: $shadow-md; display: flex; flex-direction: column; align-items: center; position: relative; }
.dcc-type-tag { position: absolute; top: $spacing-md; left: $spacing-lg; font-size: 20rpx; color: $primary; background: $bg-secondary; padding: 4rpx 16rpx; border-radius: $radius-pill; }
.dcc-word-row { display: flex; align-items: center; gap: $spacing-base; margin-bottom: $spacing-base; .dcc-word { font-size: 56rpx; font-weight: $font-weight-extrabold; color: $text-primary; } .dcc-speak { font-size: 40rpx; } }
.dcc-pos { font-size: $font-sm; color: $text-muted; } .dcc-pron { font-size: $font-sm; color: $text-secondary; font-style: italic; margin-bottom: $spacing-base; }
.dcc-kp-scroll { width: 100%; max-height: 360rpx; margin-bottom: $spacing-base; }
.dcc-kp-item { background: $bg-primary; border-radius: $radius-sm; padding: $spacing-base $spacing-md; margin-bottom: $spacing-sm; }
.dcc-kp-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6rpx; }
.dcc-kp-badge { display: inline-block; font-size: 20rpx; padding: 2rpx 12rpx; border-radius: $radius-pill; font-weight: $font-weight-semibold;
  &.pronunciation { color: #6366F1; background: #EEF2FF; } &.pos { color: #8B5CF6; background: #F5F3FF; }
  &.general_meaning { color: #059669; background: #D1FAE5; } &.extended_meaning { color: #D97706; background: #FEF3C7; }
  &.example_sentence { color: #DB2777; background: #FDF2F8; } &.memory_tip { color: #0891B2; background: #ECFEFF; } }
.dcc-kp-speak { font-size: 28rpx; opacity: 0.5; padding: 4rpx; }
.dcc-kp-content { font-size: $font-sm; color: $text-primary; line-height: 1.6; }
.dcc-meaning-toggle { background: $bg-primary; border-radius: $radius-sm; padding: $spacing-base $spacing-lg; text-align: center; cursor: pointer; }
.dcc-meaning-text { display: block; font-size: $font-base; color: $text-primary; }
.dcc-meaning-hint { display: block; font-size: 20rpx; color: $text-muted; margin-top: 4rpx; }
.dcc-actions { display: flex; gap: $spacing-md; margin-top: $spacing-xl; width: 620rpx; }
.dcc-btn { flex: 1; display: flex; flex-direction: column; align-items: center; padding: $spacing-lg 0; border-radius: $radius-md; transition: $transition-fast; &:active { transform: scale(0.94); } font-size: $font-sm; &.unknown { background: #FFE4E6; color: #E11D48; } &.fuzzy { background: #FEF3C7; color: #D97706; } &.known { background: #D1FAE5; color: #059669; } }
.dcc-marked { margin-top: $spacing-xl; display: flex; align-items: center; gap: $spacing-sm; padding: $spacing-md $spacing-xl; border-radius: $radius-pill; font-size: $font-base; &.mark-known { background: #D1FAE5; color: #059669; } &.mark-fuzzy { background: #FEF3C7; color: #D97706; } &.mark-unknown { background: #FFE4E6; color: #E11D48; } }
</style>
