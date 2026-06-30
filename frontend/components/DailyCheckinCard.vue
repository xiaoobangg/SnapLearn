<template>
  <view class="dcc-root">
    <view class="dcc-card" :class="{ 'is-new': type === 'new' }">
      <view class="dcc-type-tag">{{ type === 'new' ? '\uD83C\uDD95 \u65B0\u8BCD' : '\uD83D\uDD04 \u590D\u4E60' }}</view>
      <view class="dcc-word-row"><text class="dcc-word">{{ word.word_text }}</text><text class="dcc-speak" @click="speakWord">&#x1F50A;</text></view>
      <text class="dcc-pos" v-if="word.pos">{{ word.pos }}</text>
      <text class="dcc-pron" v-if="word.pronunciation">{{ word.pronunciation }}</text>
      <view class="dcc-meaning-toggle" @click="showMeaning = !showMeaning"><text>{{ showMeaning ? (word.general_meaning || '\u6682\u65E0\u91CA\u4E49') : '\u70B9\u51FB\u67E5\u770B\u91CA\u4E49' }}</text></view>
    </view>
    <view class="dcc-actions" v-if="!marked">
      <view class="dcc-btn unknown" @click="handleMark('unknown')"><text>\uD83D\uDE15</text><text>不认识</text></view>
      <view class="dcc-btn fuzzy" @click="handleMark('fuzzy')"><text>\uD83E\uDD14</text><text>模糊</text></view>
      <view class="dcc-btn known" @click="handleMark('known')"><text>\uD83D\uDE0A</text><text>认识</text></view>
    </view>
    <view class="dcc-marked" v-else :class="'mark-' + markValue"><text>{{ markIcon }}</text><text>{{ markLabel }}</text></view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed } from "vue"; import { getApiBaseUrl, getServerBaseUrl } from "@/config";
export interface CheckinWord { pool_id: string; word_text: string; pronunciation?: string; pos?: string; general_meaning?: string; }
const props = defineProps<{ word: CheckinWord; type: 'new' | 'review'; showSelect?: boolean; }>();
const emit = defineEmits<{ mark: [poolId: string, mark: string]; toggleSelect: [wordText: string]; }>();
const showMeaning = ref(false); const marked = ref(false); const markValue = ref("");
const markIcon = computed(() => ({ known: "\uD83D\uDE0A", fuzzy: "\uD83E\uDD14", unknown: "\uD83D\uDE15" }[markValue.value] || ""));
const markLabel = computed(() => ({ known: "已标记：认识", fuzzy: "已标记：模糊", unknown: "已标记：不认识" }[markValue.value] || ""));
function handleMark(mark: string) { markValue.value = mark; marked.value = true; emit("mark", props.word.pool_id, mark); }
function speakWord() {
  const text = props.word.word_text; if (window.speechSynthesis) { const u = new SpeechSynthesisUtterance(text); u.lang = "en-US"; u.rate = 0.85; window.speechSynthesis.speak(u); return; }
  uni.request({ url: `${getApiBaseUrl()}/tts`, data: { text, lang: "en" }, success: (res: any) => { if (res.data?.url) { const audio = uni.createInnerAudioContext(); audio.src = getServerBaseUrl() + res.data.url; audio.play(); } } });
}
</script>
<style lang="scss" scoped>
.dcc-root { display: flex; flex-direction: column; align-items: center; padding: $spacing-lg 0; }
.dcc-card { width: 620rpx; background: $bg-card; border-radius: $radius-lg; padding: 48rpx 40rpx; box-shadow: $shadow-md; display: flex; flex-direction: column; align-items: center; position: relative; }
.dcc-type-tag { position: absolute; top: $spacing-md; left: $spacing-lg; font-size: 20rpx; color: $primary; background: $bg-secondary; padding: 4rpx 16rpx; border-radius: $radius-pill; }
.dcc-word-row { display: flex; align-items: center; gap: $spacing-base; margin-bottom: $spacing-base; .dcc-word { font-size: 56rpx; font-weight: $font-weight-extrabold; color: $text-primary; } .dcc-speak { font-size: 40rpx; } }
.dcc-pos { font-size: $font-sm; color: $text-muted; } .dcc-pron { font-size: $font-sm; color: $text-secondary; font-style: italic; margin-bottom: $spacing-base; }
.dcc-meaning-toggle { background: $bg-primary; border-radius: $radius-sm; padding: $spacing-base $spacing-lg; min-width: 300rpx; text-align: center; font-size: $font-base; }
.dcc-actions { display: flex; gap: $spacing-md; margin-top: $spacing-xl; width: 620rpx; }
.dcc-btn { flex: 1; display: flex; flex-direction: column; align-items: center; padding: $spacing-lg 0; border-radius: $radius-md; transition: $transition-fast; &:active { transform: scale(0.94); } font-size: $font-sm; &.unknown { background: #FFE4E6; color: #E11D48; } &.fuzzy { background: #FEF3C7; color: #D97706; } &.known { background: #D1FAE5; color: #059669; } }
.dcc-marked { margin-top: $spacing-xl; display: flex; align-items: center; gap: $spacing-sm; padding: $spacing-md $spacing-xl; border-radius: $radius-pill; font-size: $font-base; &.mark-known { background: #D1FAE5; color: #059669; } &.mark-fuzzy { background: #FEF3C7; color: #D97706; } &.mark-unknown { background: #FFE4E6; color: #E11D48; } }
</style>
