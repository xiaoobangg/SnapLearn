<template>
  <view class="test-page">
    <view class="test-header"><text class="th-back" @click="goBack">&#x2190;</text><text class="th-title">&#x1F4DD; {{ groupName }}</text></view>
    <view class="progress-wrap" v-if="questions.length > 0">
      <view class="progress-bar">
        <view v-for="(q, qi) in questions" :key="'pb-' + q.id" class="pb-seg" :class="segClass(q, qi)" :style="{ width: (100/questions.length) + '%' }" />
      </view>
      <text class="progress-text">{{ answeredCount }} / {{ questions.length }}</text>
    </view>
    <scroll-view scroll-y class="test-body" v-if="questions.length > 0" :style="{ height: scrollHeight + 'px' }">
      <TestQuestion v-if="questions[currentIndex]" :key="questions[currentIndex].id" :question="questions[currentIndex]" :index="currentIndex" :total="questions.length" :show-result="questions[currentIndex].question_type === 'spelling' ? showResult : answeredMap[questions[currentIndex].id] !== undefined" :selected-answer="answeredMap[questions[currentIndex].id]" @answer="handleAnswer" />
    </scroll-view>
    <view class="test-loading" v-else-if="questions.length === 0"><text>✨ 正在生成题目...</text></view>
    <view class="test-result" v-if="showResult">
      <view class="tr-icon-wrap"><text>{{ result.all_correct ? '🎉' : '💪' }}</text></view>
      <text class="tr-title">{{ result.all_correct ? '全部正确！' : '继续加油！' }}</text>
      <text class="tr-auto-hint">即将自动返回...</text>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api"; import TestQuestion from "@/components/TestQuestion.vue";
const groupId = ref(""); const groupName = ref(""); const questions = ref<any[]>([]); const showResult = ref(false);
const result = ref({ total:0, correct:0, all_correct:false, round:0, error_card_ids:[] as string[] });
const answeredMap = reactive<Record<string, string>>({});
const questionStatus = reactive<Record<string, string>>({});
const currentIndex = ref(0);
const scrollHeight = ref(500);
const answeredCount = computed(() => Object.keys(answeredMap).length);
const allAnswered = computed(() => questions.value.length > 0 && questions.value.every(q => answeredMap[q.id]));
function segClass(q: any, idx: number) {
  const status = questionStatus[q.id];
  let cls = "seg-pending";
  if (status === "correct") cls = "seg-done";
  if (status === "wrong") cls = "seg-wrong";
  if (idx === currentIndex.value) cls += " seg-current";
  return cls;
}
onLoad(async (o:any) => { groupId.value = o?.groupId||""; groupName.value = decodeURIComponent(o?.groupName||"测验"); await loadQuestions(); });
onMounted(() => { const sys = uni.getSystemInfoSync(); scrollHeight.value = (sys.windowHeight || 667) - 160; });
async function loadQuestions() { try { const res = await api.startTest(groupId.value); questions.value = res.questions || []; } catch(_e) { uni.showToast({ title:"加载失败", icon:"none" }); } }
function handleAnswer(qid:string, a:string) {
  answeredMap[qid] = a;
  const q = questions.value.find((q: any) => q.id === qid);
  if (q && a === q.correct_answer) {
    questionStatus[qid] = "correct";
    setTimeout(() => { if (currentIndex.value < questions.value.length - 1) currentIndex.value++; }, 500);
  } else {
    questionStatus[qid] = "wrong";
  }
}
async function submitTest() {
  try {
    result.value = await api.submitTest(groupId.value, Object.entries(answeredMap).map(([q,u])=>({questionId:q, userAnswer:u})));
    showResult.value = true;
    // 自动返回
    setTimeout(() => { uni.navigateBack(); }, 1500);
  } catch(_e) { uni.showToast({ title:"提交失败", icon:"none" }); }
}
// 全部答完自动提交
watch(allAnswered, (v) => { if (v && !showResult.value) submitTest(); });
function goBack() { uni.navigateBack(); }
</script>
<style lang="scss" scoped>
.test-page { min-height: 100vh; background: $gradient-bg; display: flex; flex-direction: column; }
.test-header { display: flex; align-items: center; gap: $spacing-base; padding: $spacing-lg $spacing-xl $spacing-base; background: $bg-card; border-bottom: 2rpx solid $border-light; flex-shrink: 0; .th-back { font-size: $font-xl; color: $primary; width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; background: $bg-secondary; border-radius: 50%; } .th-title { font-size: $font-lg; font-weight: $font-weight-bold; flex: 1; } }
.progress-wrap { display: flex; align-items: center; gap: 10rpx; padding: $spacing-sm $spacing-xl; background: $bg-card; flex-shrink: 0; }
.progress-bar { flex: 1; height: 28rpx; background: #F3F4F6; border-radius: 14rpx; overflow: visible; display: flex; position: relative; }
.pb-seg { height: 100%; position: relative; transition: background 0.3s; &:first-child { border-radius: 14rpx 0 0 14rpx; } &:last-child { border-radius: 0 14rpx 14rpx 0; } &.seg-current { &::after { content: "😊"; position: absolute; top: -25rpx; left: 50%; transform: translateX(-50%); font-size: 45rpx; } } &.seg-done { background: #10B981; } &.seg-wrong { background: #F59E0B; } &.seg-pending { background: #E5E7EB; } }
.progress-text { font-size: $font-sm; color: $text-secondary; font-weight: $font-weight-bold; flex-shrink: 0; }
.test-body { flex: 1; padding: 0 $spacing-xl 200rpx; }
.test-bottom { padding: $spacing-base $spacing-xl 40rpx; flex-shrink: 0; background: $bg-card; .btn-submit { width: 100%; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 24rpx 0; font-size: $font-lg; font-weight: $font-weight-semibold; } }
.test-result { padding: 60rpx $spacing-xl; display: flex; flex-direction: column; align-items: center; .tr-icon-wrap { width: 140rpx; height: 140rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; margin-bottom: $spacing-lg; font-size: 64rpx; } .tr-title { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; } .tr-score { font-size: $font-hero; font-weight: $font-weight-extrabold; color: $primary; } .tr-auto-hint { margin-top: $spacing-base; font-size: $font-sm; color: $text-secondary; } }
</style>
