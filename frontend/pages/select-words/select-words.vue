<template>
  <view class="select-page">
    <view class="preview-section" v-if="imagePath"><image class="preview-image" :src="imagePath" mode="aspectFit" /></view>
    <view class="content-section">
      <view class="loading-box" v-if="loading"><text>&#x1F4E1; AI 正在识别文字...</text></view>
      <view class="ocr-result" v-else-if="words.length > 0">
        <view class="result-toolbar"><text>&#x1F50D; 识别结果</text><text class="btn-select-all" @click="selectAll">{{ selected.size === words.length ? '\u53D6\u6D88\u5168\u9009' : '\u5168\u9009' }}</text></view>
        <view class="word-grid">
          <view v-for="(word, idx) in words" :key="idx" class="word-tag" :class="{ selected: selected.has(word) }">
            <text @click="toggleWord(word)">{{ word }}</text>
            <text class="tag-btn" @click.stop="removeWord(idx)">&#x2715;</text>
          </view>
        </view>
        <view class="add-word-bar"><input class="add-input" v-model="newWord" placeholder="添加单词..." @confirm="addWord" /><view class="btn-add" @click="addWord" v-if="newWord.trim()">添加</view></view>
      </view>
    </view>
    <view class="bottom-bar">
      <view class="bar-left" v-if="selected.size > 0"><text>&#x2705; 已选 {{ selected.size }} 个</text></view>
      <button class="btn-generate" :disabled="selected.size === 0" @click="generateCards">&#x2728; 生成学习卡片</button>
    </view>
    <view class="generating-overlay" v-if="generating"><text>AI 正在生成学习卡片...</text></view>
    <view class="success-overlay" v-if="showSuccess">
      <view class="success-card"><view class="success-ring"><text>&#x2728;</text></view><text>卡片生成成功！</text><text>已为你创建 {{ createdCardCount }} 张学习卡片</text><button class="btn-start-learn" @click="goCardDetail">&#x1F4DA; 开始学习</button></view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api";
const imagePath = ref(""); const loading = ref(true); const ocrText = ref(""); const words = ref<string[]>([]); const selected = ref(new Set<string>());
const generating = ref(false); const showSuccess = ref(false); const createdGroupId = ref(""); const createdCardCount = ref(0);
const newWord = ref(""); const editIdx = ref(-1); const editWord = ref("");
onLoad((o:any)=>{ if(o?.manualWords) { words.value=decodeURIComponent(o.manualWords).split(",").filter(Boolean); loading.value=false; } else if(o?.imagePath) { imagePath.value=decodeURIComponent(o.imagePath); doOCR(); } });
async function doOCR() { loading.value=true; try { const r=await api.recognizeImage(imagePath.value); ocrText.value=r.text; words.value=r.words; } catch(_e) { uni.showToast({title:"识别失败", icon:"none"}); } finally { loading.value=false; } }
function toggleWord(w:string) { const s=new Set(selected.value); if(s.has(w)) s.delete(w); else s.add(w); selected.value=s; }
function selectAll() { selected.value = new Set(selected.value.size===words.value.length ? [] : words.value); }
function removeWord(idx:number) { const w=words.value[idx]; const s=new Set(selected.value); s.delete(w); selected.value=s; words.value.splice(idx,1); }
function addWord() { const w=newWord.value.trim(); if(!w||words.value.includes(w)) { newWord.value=""; return; } words.value.push(w); newWord.value=""; }
function goHome() { uni.switchTab({url:"/pages/index/index"}); }
async function generateCards() { if(selected.value.size===0) return; generating.value=true; try { const r=await api.createCardGroup({source_text:ocrText.value,selected_words:[...selected.value]}); createdGroupId.value=r.id; createdCardCount.value=selected.value.size; showSuccess.value=true; generating.value=false; } catch(_e) { uni.showToast({title:"生成失败",icon:"none"}); generating.value=false; } }
function goCardDetail() { if(createdGroupId.value) uni.navigateTo({url:`/pages/card-detail/card-detail?groupId=${createdGroupId.value}`}); }
</script>
<style lang="scss" scoped>
.select-page { min-height: 100vh; background: $bg-primary; }
.preview-section { background: #000; border-radius: 0 0 $radius-md $radius-md; overflow: hidden; }
.content-section { flex: 1; padding: $spacing-xl; }
.loading-box { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; font-size: $font-base; color: $text-secondary; }
.result-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-base; .btn-select-all { font-size: $font-sm; color: $primary; } }
.word-grid { display: flex; flex-wrap: wrap; gap: $spacing-base; margin-bottom: $spacing-md; }
.word-tag { display: flex; align-items: center; gap: 8rpx; padding: 8rpx 12rpx 8rpx 24rpx; background: $bg-card; border: 2rpx solid $border-color; border-radius: $radius-pill; font-size: $font-base; transition: all 0.2s; &.selected { background: $gradient-primary; color: #fff; border-color: transparent; } }
.tag-btn { width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center; border-radius: 50%; font-size: 22rpx; color: $text-muted; &:active { color: $danger; background: #FFE4E6; } }
.add-word-bar { display: flex; gap: $spacing-sm; margin-bottom: $spacing-lg; .add-input { flex: 1; height: 64rpx; background: $bg-card; border: 2rpx dashed $border-color; border-radius: $radius-pill; padding: 0 $spacing-lg; font-size: $font-base; } }
.bottom-bar { position: sticky; bottom: 0; background: $bg-card; padding: $spacing-lg $spacing-xl; padding-bottom: calc(24rpx + env(safe-area-inset-bottom)); display: flex; align-items: center; justify-content: flex-end; border-top: 2rpx solid $border-light; box-shadow: 0 -4rpx 20rpx rgba(255,138,155,0.1); }
.btn-generate { background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 22rpx 40rpx; font-size: $font-base; font-weight: $font-weight-semibold; box-shadow: $shadow-sm; &[disabled] { opacity: 0.4; box-shadow: none; } }
.generating-overlay { position: fixed; inset: 0; background: $bg-overlay; backdrop-filter: blur(16rpx); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.gen-card { background: $bg-card; border-radius: $radius-xl; padding: 56rpx 44rpx; display: flex; flex-direction: column; align-items: center; box-shadow: $shadow-lg; }
.success-overlay { position: fixed; inset: 0; background: $bg-overlay; backdrop-filter: blur(12rpx); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.success-card { background: $bg-card; border-radius: $radius-xl; padding: 64rpx 48rpx; display: flex; flex-direction: column; align-items: center; box-shadow: $shadow-lg; }
.success-ring { width: 140rpx; height: 140rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; margin-bottom: $spacing-xl; }
.btn-start-learn { width: 100%; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 26rpx; font-size: $font-lg; font-weight: $font-weight-bold; box-shadow: $shadow-sm; margin-bottom: $spacing-md; }
</style>
