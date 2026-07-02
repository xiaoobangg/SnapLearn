<template>
  <view class="test-page">
    <view class="test-header"><text class="th-back" @click="goBack">&#x2190;</text><text class="th-title">&#x1F3B2; 随机测验</text></view>
    <view class="progress-wrap" v-if="questions.length > 0">
      <view class="progress-bar">
        <view v-for="(q, qi) in questions" :key="'pb-' + q.id" class="pb-seg" :class="segClass(q, qi)" :style="{ width: (100/questions.length) + '%' }" />
      </view>
      <text class="progress-text">{{ answeredCount }} / {{ questions.length }}</text>
    </view>
    <scroll-view scroll-y class="test-body" v-if="questions.length > 0" :style="{ height: scrollHeight + 'px' }">
      <TestQuestion v-if="questions[currentIndex]" :key="questions[currentIndex].id" :question="questions[currentIndex]" :index="currentIndex" :total="questions.length" :show-result="questions[currentIndex].question_type === 'spelling' ? false : answeredMap[questions[currentIndex].id] !== undefined" :selected-answer="answeredMap[questions[currentIndex].id]" @answer="handleAnswer" />
    </scroll-view>
    <view class="test-loading" v-if="generating"><text>⏳ 正在生成题目...</text></view>
    <view class="test-loading" v-else-if="questions.length === 0 && !generating"><text>&#x1F3B2; 点击下方按钮开始随机测验</text></view>
    <view class="test-confirm-bar" v-if="canConfirm && !submitted">
      <text class="tcb-info" v-if="wrongCount > 0">本次共错 {{ wrongCount }} 题</text>
      <text class="tcb-info" v-else>全部正确 &#x1F389;</text>
      <button class="tcb-btn" @click="confirmAndClose">确认</button>
    </view>
    <view class="start-bar" v-if="questions.length === 0 && !generating">
      <button class="start-btn" @click="startTest">&#x1F3B2; 开始随机测验</button>
      <text class="start-hint">从你的测试池中随机抽取 10 道题目</text>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue"; import { api } from "@/api"; import TestQuestion from "@/components/TestQuestion.vue";
const questions = ref<any[]>([]); const generating = ref(false); const submitted = ref(false);
const answeredMap = reactive<Record<string, string>>({});
const questionStatus = reactive<Record<string, string>>({});
const currentIndex = ref(0);
const scrollHeight = ref(500);
const answeredCount = computed(() => Object.keys(answeredMap).length);
const allAnswered = computed(() => questions.value.length > 0 && questions.value.every(q => answeredMap[q.id]));
const allCorrect = computed(() => questions.value.length > 0 && questions.value.every(q => questionStatus[q.id] === "correct"));
const canConfirm = computed(() => allAnswered.value && allCorrect.value);
const wrongCount = computed(() => Object.values(questionStatus).filter(s => s === "wrong").length);
function segClass(q: any, idx: number) {
  const status = questionStatus[q.id];
  let cls = "seg-pending";
  if (status === "correct") cls = "seg-done";
  if (status === "wrong") cls = "seg-wrong";
  if (idx === currentIndex.value) cls += " seg-current";
  return cls;
}
onMounted(() => { const sys = uni.getSystemInfoSync(); scrollHeight.value = (sys.windowHeight || 667) - 160; });
async function startTest() {
  generating.value = true;
  try {
    const res = await api.startRandomTest(10);
    if (!res.ok) { uni.showToast({ title: res.message || "暂无题目", icon: "none" }); generating.value = false; return; }
    questions.value = res.questions || [];
    Object.keys(answeredMap).forEach(k => delete answeredMap[k]);
    Object.keys(questionStatus).forEach(k => delete questionStatus[k]);
    currentIndex.value = 0;
    generating.value = false;
  } catch (_e) { uni.showToast({ title: "加载失败", icon: "none" }); generating.value = false; }
}
function handleAnswer(qid:string, a:string) {
  answeredMap[qid] = a;
  const q = questions.value.find((q: any) => q.id === qid);
  if (!q) return;
  if (a === q.correct_answer) {
    questionStatus[qid] = "correct";
    setTimeout(() => { if (currentIndex.value < questions.value.length - 1) currentIndex.value++; }, 500);
  } else if (q.question_type === 'spelling' && q.correct_answer.startsWith(a)) {
    // 拼写题：当前输入是正确答案的前缀，还在构建中，不判错
    return;
  } else {
    questionStatus[qid] = "wrong";
    api.markWrong({ word_id: q.word_id, question_type: q.question_type }).catch(() => {});
  }
}
async function confirmAndClose() {
  if (submitted.value) return;
  submitted.value = true;
  try {
    await api.submitRandomTest({
      question_ids: questions.value.map(q => q.id),
      user_answers: questions.value.map(q => answeredMap[q.id] || ""),
    });
  } catch (_e) {
    uni.showToast({ title: "提交失败", icon: "none" });
    submitted.value = false;
    return;
  }
  uni.navigateBack();
}
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
.test-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 120rpx 0; color: $text-secondary; font-size: $font-lg; }
.test-confirm-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: $spacing-lg $spacing-xl 40rpx; background: $bg-card; border-top: 2rpx solid $border-light; display: flex; align-items: center; justify-content: space-between; gap: $spacing-base; z-index: 100; .tcb-info { font-size: $font-base; color: $text-primary; font-weight: $font-weight-semibold; } .tcb-btn { padding: 16rpx 48rpx; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; font-size: $font-base; font-weight: $font-weight-semibold; } }
.start-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: $spacing-xl; display: flex; flex-direction: column; align-items: center; gap: $spacing-sm; .start-btn { width: 80%; padding: 24rpx; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; font-size: $font-lg; font-weight: $font-weight-bold; box-shadow: $shadow-sm; } .start-hint { font-size: $font-sm; color: $text-muted; } }
</style>
