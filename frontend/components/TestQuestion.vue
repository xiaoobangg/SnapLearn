<template>
  <view class="tq-root">
    <view class="tq-header"><view class="tq-progress-wrap"><text class="tq-progress-num">{{ index + 1 }}</text><text>/ {{ total }}</text></view><view class="tq-type-badge">{{ typeLabel }}</view></view>
    <view class="tq-question-card"><text>{{ question.question_text }}</text></view>
    <view class="tq-options" v-if="isSpelling">
      <view class="spell-hint"><text>👇 点击多个选项组合成正确单词</text></view>
      <view class="spell-preview" v-if="selectedFragments.length > 0"><text>&#x270D; 你的拼写：</text><text class="sp-preview-word">{{ composedWord }}</text></view>
      <view v-for="(opt, oi) in parsedOptions" :key="'opt-' + oi" class="tq-option" :class="spellOptClass(opt)" @click="toggleFragment(opt)"><view class="tq-option-marker" :class="{ checked: selectedFragments.includes(opt) }">{{ selectedFragments.includes(opt) ? '✓' : optionLabel(oi) }}</view><text>{{ opt }}</text></view>
    </view>
    <view class="tq-options" v-else>
      <view v-for="(opt, oi) in parsedOptions" :key="'opt-' + oi" class="tq-option" :class="{ selected: localAnswer === opt && !showResult, correct: isCorrect && opt === question.correct_answer, wrong: showResult && localAnswer === opt && opt !== question.correct_answer }" @click="selectOption(opt)"><view class="tq-option-marker">{{ optionLabel(oi) }}</view><text>{{ opt }}</text></view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed } from "vue";
export interface Question { id: string; question_type: string; question_text: string; options: string; correct_answer: string; }
const props = defineProps<{ question: Question; index: number; total: number; showResult?: boolean; selectedAnswer?: string; }>();
const emit = defineEmits<{ answer: [questionId: string, userAnswer: string]; }>();
const localAnswer = ref(props.selectedAnswer || "");
const selectedFragments = ref<string[]>([]);
let _lastQid = "";
const parsedOptions = computed(() => {
  if (props.question.id !== _lastQid) { _lastQid = props.question.id; localAnswer.value = props.selectedAnswer || ""; selectedFragments.value = []; }
  try { const p = JSON.parse(props.question.options); return Array.isArray(p) ? p : []; } catch (_e) { return []; }
});
const isSpelling = computed(() => props.question.question_type === "spelling");
const composedWord = computed(() => selectedFragments.value.join(""));
const typeLabel = computed(() => ({ meaning_select: "看单词选释义", word_select: "看释义选单词", collocation: "组词搭配", spelling: "拼写组合" }[props.question.question_type] || ""));
	const isCorrect = computed(() => {
		const ok = isSpelling.value ? composedWord.value === props.question.correct_answer : localAnswer.value === props.question.correct_answer;
		return ok;
	});
function optionLabel(i: number): string { return String.fromCharCode(65 + i); }
function selectOption(opt: string) { localAnswer.value = opt; emit("answer", props.question.id, opt); }
function toggleFragment(opt: string) { if (props.showResult) return; const idx = selectedFragments.value.indexOf(opt); if (idx >= 0) selectedFragments.value.splice(idx, 1); else selectedFragments.value.push(opt); emit("answer", props.question.id, selectedFragments.value.join("")); }
function spellOptClass(opt: string) {
  const isPartOfAnswer = props.question.correct_answer.includes(opt);
  const isSelected = selectedFragments.value.includes(opt);
  if (isCorrect.value) return { correct: isSelected, 'correct-unselected': !isSelected && isPartOfAnswer };
  if (props.showResult) return { wrong: isSelected && !isPartOfAnswer };
  return { selected: isSelected };
}
</script>
<style lang="scss" scoped>
.tq-root { padding: $spacing-lg $spacing-xl; }
.tq-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-xl; .tq-progress-wrap { display: flex; align-items: baseline; gap: 2rpx; .tq-progress-num { font-size: $font-xl; font-weight: $font-weight-bold; color: $primary; } } .tq-type-badge { font-size: 22rpx; color: $text-secondary; background: $bg-secondary; padding: 6rpx 18rpx; border-radius: $radius-pill; } }
.tq-question-card { background: $bg-card; border-radius: $radius-md; padding: 40rpx $spacing-xl; box-shadow: $shadow-sm; margin-bottom: $spacing-lg; font-size: $font-lg; color: $text-primary; font-weight: $font-weight-semibold; line-height: 1.6; }
.tq-options { display: flex; flex-direction: column; gap: $spacing-base; }
.tq-option { display: flex; align-items: center; background: $bg-card; border-radius: $radius-md; padding: $spacing-lg; border: 2rpx solid $border-color; transition: $transition-base; &:active { transform: scale(0.98); } &.selected { border-color: $primary; background: $bg-secondary; } &.correct { border-color: $success; background: #D1FAE5; } &.wrong { border-color: $danger; background: #FFE4E6; } &.correct-unselected { border-color: $success; background: #D1FAE5; opacity: 0.5; } }
.tq-option-marker { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; border-radius: 50%; background: $bg-primary; font-size: $font-sm; font-weight: $font-weight-bold; color: $text-secondary; flex-shrink: 0; margin-right: $spacing-base; &.checked { background: $primary; color: #fff; } }
.spell-hint { font-size: 22rpx; color: $text-muted; margin-bottom: $spacing-sm; }
.spell-preview { display: flex; align-items: center; gap: $spacing-sm; background: $bg-secondary; border-radius: $radius-md; padding: $spacing-md $spacing-lg; margin-bottom: $spacing-md; .sp-preview-word { font-size: $font-xl; font-weight: $font-weight-extrabold; color: $primary; letter-spacing: 4rpx; } }
</style>
