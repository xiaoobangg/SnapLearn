<template>
  <view class="camera-page">
    <camera v-if="showCamera" class="camera" device-position="back" flash="auto" @error="onCameraError" />
    <view class="camera-top">
      <view class="btn-close" @click="goHome"><text>&#x2715;</text></view>
      <text class="camera-title">拍照识词</text>
    </view>
    <view class="camera-overlay"><text>&#x1F4F7; 将课本/文章对准取景框</text></view>
    <view class="camera-controls">
      <view class="btn-gallery" @click="pickFromAlbum"><text>&#x1F5BC; 相册</text></view>
      <view class="btn-capture" @click="takePhoto"><view class="capture-circle" /></view>
      <view class="btn-placeholder" />
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from "vue";
const showCamera = ref(true);
function onCameraError(e: any) { uni.showToast({ title: "相机启动失败，请检查权限", icon: "none" }); }
function takePhoto() {
  const ctx = uni.createCameraContext();
  ctx.takePhoto({ quality: "high", success: (res) => { uni.redirectTo({ url: `/pages/select-words/select-words?imagePath=${encodeURIComponent(res.tempImagePath)}` }); }, fail: () => { uni.showToast({ title: "拍照失败", icon: "none" }); } });
}
function goHome() { uni.switchTab({ url: "/pages/index/index" }); }
function pickFromAlbum() {
  uni.chooseImage({ count: 1, sizeType: ["compressed"], sourceType: ["album"], success: (res) => { uni.redirectTo({ url: `/pages/select-words/select-words?imagePath=${encodeURIComponent(res.tempFilePaths[0])}` }); } });
}
</script>
<style lang="scss" scoped>
.camera-page { position: fixed; inset: 0; background: #000; }
.camera { width: 100vw; height: 100vh; }
.camera-top { position: absolute; top: 0; left: 0; right: 0; display: flex; align-items: center; justify-content: space-between; padding: 48rpx 32rpx 24rpx; padding-top: calc(48rpx + env(safe-area-inset-top)); z-index: 10; .btn-close { width: 64rpx; height: 64rpx; border-radius: 50%; background: rgba(255,255,255,0.2); backdrop-filter: blur(10rpx); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32rpx; } .camera-title { font-size: $font-lg; font-weight: $font-weight-bold; color: #fff; text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.3); } }
.camera-overlay { position: absolute; top: 180rpx; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,0.35); backdrop-filter: blur(8rpx); border-radius: $radius-pill; padding: 16rpx 36rpx; color: #fff; font-size: $font-base; }
.camera-controls { position: absolute; bottom: 0; left: 0; right: 0; display: flex; align-items: center; justify-content: space-around; padding: 48rpx 40rpx; padding-bottom: calc(48rpx + env(safe-area-inset-bottom)); background: linear-gradient(transparent, rgba(0,0,0,0.4));
  .btn-gallery { width: 160rpx; text-align: center; .btn-text { color: #fff; font-size: $font-base; background: rgba(255,255,255,0.15); padding: 12rpx 24rpx; border-radius: $radius-pill; } }
  .btn-placeholder { width: 160rpx; }
}
.btn-capture { width: 148rpx; height: 148rpx; border-radius: 50%; border: 6rpx solid rgba(255,255,255,0.4); display: flex; align-items: center; justify-content: center; transition: $transition-fast; &:active { transform: scale(0.92); } .capture-circle { width: 114rpx; height: 114rpx; border-radius: 50%; background: linear-gradient(135deg, #FFB3C1, #FF8A9B); box-shadow: 0 4rpx 20rpx rgba(255,138,155,0.3); } }
</style>
