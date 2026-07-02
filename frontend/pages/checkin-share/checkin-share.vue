<template>
  <view class="share-page">
    <!-- 预览卡片 -->
    <view class="preview-card" id="previewCard">
      <view class="card-bg">
        <view class="card-inner">
          <view class="card-header">
            <text class="card-logo">SnapLearn</text>
            <text class="card-subtitle">拍立学 · 每日打卡</text>
          </view>

          <view class="streak-section">
            <text class="streak-number">{{ stats.consecutive_days }}</text>
            <text class="streak-label">连续打卡天数</text>
          </view>

          <view class="divider" />

          <view class="stats-row">
            <view class="stat-item">
              <text class="stat-num">{{ stats.total_checkin_days }}</text>
              <text class="stat-text">累计打卡</text>
            </view>
            <view class="stat-item">
              <text class="stat-num">{{ stats.total_pool_words }}</text>
              <text class="stat-text">单词总数</text>
            </view>
            <view class="stat-item">
              <text class="stat-num">{{ stats.mastered_count }}</text>
              <text class="stat-text">已掌握</text>
            </view>
          </view>

          <!-- 本月热力图 -->
          <view class="calendar-section" v-if="calendarDays.length > 0">
            <text class="cal-title">{{ currentMonth }}</text>
            <view class="cal-grid">
              <view v-for="(d, i) in calendarDays" :key="i"
                    :class="['cal-day', d.checked ? 'cal-checked' : 'cal-empty']" />
            </view>
          </view>

          <view class="card-footer">
            <text class="footer-text">{{ todayDate }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="actions">
      <button class="btn-save" @click="saveImage" :loading="saving">
        {{ saving ? '生成中...' : '保存图片' }}
      </button>
      <text class="save-hint">长按图片可保存或分享给好友</text>
    </view>

    <!-- 隐藏 canvas -->
    <canvas canvas-id="shareCanvas"
            :style="{ width: canvasW + 'px', height: canvasH + 'px', position: 'fixed', left: '-9999px', top: 0 }" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "@/api";

const stats = ref({ consecutive_days: 0, total_checkin_days: 0, total_pool_words: 0, mastered_count: 0 });
const calendarDays = ref<{ checked: boolean }[]>([]);
const currentMonth = ref("");
const todayDate = ref("");
const saving = ref(false);
const canvasW = 375 * 2;  // 750rpx → 375px * 2 for retina
const canvasH = 500 * 2;

onMounted(async () => {
  const now = new Date();
  todayDate.value = `${now.getFullYear()}/${now.getMonth() + 1}/${now.getDate()}`;
  currentMonth.value = `${now.getFullYear()}年${now.getMonth() + 1}月`;

  try {
    const [s, cal] = await Promise.all([
      api.getCheckinStats(),
      api.getCheckinCalendar(now.getFullYear(), now.getMonth() + 1),
    ]);
    stats.value = {
      consecutive_days: s.consecutive_days || 0,
      total_checkin_days: s.total_checkin_days || 0,
      total_pool_words: s.total_pool_words || 0,
      mastered_count: s.mastered_count || 0,
    };

    // Build month calendar grid: pad to start on correct weekday
    const checkinDays = new Set(cal.checkin_days || []);
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).getDay();
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const grid: { checked: boolean }[] = [];
    for (let i = 0; i < firstDay; i++) grid.push({ checked: false });
    for (let d = 1; d <= daysInMonth; d++) {
      const ds = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      grid.push({ checked: checkinDays.has(ds) });
    }
    calendarDays.value = grid;
  } catch (_) { /* ignore */ }
});

async function saveImage() {
  saving.value = true;
  try {
    // #ifdef MP-WEIXIN
    const ctx = uni.createCanvasContext('shareCanvas');
    const w = 375;
    const h = 500;

    // Background gradient
    const gradient = ctx.createLinearGradient(0, 0, 0, h);
    gradient.addColorStop(0, '#667eea');
    gradient.addColorStop(1, '#764ba2');
    ctx.setFillStyle(gradient);
    ctx.fillRect(0, 0, w, h);

    // White card
    ctx.setFillStyle('rgba(255,255,255,0.15)');
    ctx.fillRect(16, 16, w - 32, h - 32);
    ctx.setFillStyle('#ffffff');

    // Header
    ctx.setFontSize(22);
    ctx.setTextAlign('center');
    ctx.fillText('SnapLearn 拍立学', w / 2, 70);
    ctx.setFontSize(13);
    ctx.setFillStyle('rgba(255,255,255,0.7)');
    ctx.fillText('每日打卡', w / 2, 95);

    // Streak
    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(64);
    ctx.setTextAlign('center');
    const streak = String(stats.value.consecutive_days);
    ctx.fillText(streak, w / 2, 170);
    ctx.setFontSize(16);
    ctx.setFillStyle('rgba(255,255,255,0.8)');
    ctx.fillText('连续打卡天数', w / 2, 200);

    // Divider
    ctx.setStrokeStyle('rgba(255,255,255,0.3)');
    ctx.setLineWidth(1);
    ctx.moveTo(50, 220);
    ctx.lineTo(w - 50, 220);
    ctx.stroke();

    // Stats row
    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(28);
    ctx.setTextAlign('center');
    ctx.fillText(String(stats.value.total_checkin_days), w / 4, 260);
    ctx.fillText(String(stats.value.total_pool_words), w / 2, 260);
    ctx.fillText(String(stats.value.mastered_count), w * 3 / 4, 260);

    ctx.setFontSize(12);
    ctx.setFillStyle('rgba(255,255,255,0.7)');
    ctx.fillText('累计打卡', w / 4, 282);
    ctx.fillText('单词总数', w / 2, 282);
    ctx.fillText('已掌握', w * 3 / 4, 282);

    // Calendar label
    ctx.setFillStyle('rgba(255,255,255,0.8)');
    ctx.setFontSize(14);
    ctx.fillText(currentMonth.value, w / 2, 320);

    // Calendar grid
    const cx = 40, cy = 335, cell = 18, gap = 4;
    calendarDays.value.forEach((d, i) => {
      const col = i % 7;
      const row = Math.floor(i / 7);
      const x = cx + col * (cell + gap);
      const y = cy + row * (cell + gap);
      ctx.setFillStyle(d.checked ? '#FFD700' : 'rgba(255,255,255,0.2)');
      ctx.fillRect(x, y, cell, cell);
    });

    // Footer
    ctx.setFillStyle('rgba(255,255,255,0.5)');
    ctx.setFontSize(11);
    ctx.setTextAlign('center');
    ctx.fillText(todayDate.value, w / 2, h - 30);

    ctx.draw(false, () => {
      uni.canvasToTempFilePath({
        canvasId: 'shareCanvas',
        success: (res: any) => {
          uni.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => {
              uni.showToast({ title: '已保存到相册', icon: 'success' });
            },
            fail: () => {
              uni.showToast({ title: '保存失败，请重试', icon: 'none' });
            },
          });
        },
        fail: () => {
          uni.showToast({ title: '生成图片失败', icon: 'none' });
        },
      });
    });
    // #endif
  } catch (_) {
    uni.showToast({ title: '生成失败', icon: 'none' });
  }
  saving.value = false;
}
</script>

<style lang="scss" scoped>
.share-page {
  min-height: 100vh;
  background: $gradient-bg;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
}

.preview-card {
  width: 630rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(102, 126, 234, 0.3);
  margin-bottom: 40rpx;
}

.card-bg {
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 8rpx;
}

.card-inner {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 18rpx;
  padding: 48rpx 36rpx 36rpx;
}

.card-header {
  text-align: center;
  margin-bottom: 32rpx;
}

.card-logo {
  font-size: 40rpx;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2rpx;
}

.card-subtitle {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 6rpx;
}

.streak-section {
  text-align: center;
  margin-bottom: 28rpx;
}

.streak-number {
  font-size: 120rpx;
  font-weight: 900;
  color: #FFD700;
  line-height: 1;
  text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}

.streak-label {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

.divider {
  height: 2rpx;
  background: rgba(255, 255, 255, 0.2);
  margin: 28rpx 0;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 32rpx;
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.stat-text {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4rpx;
}

.calendar-section {
  margin-bottom: 24rpx;
}

.cal-title {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  margin-bottom: 12rpx;
}

.cal-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6rpx;
}

.cal-day {
  width: 32rpx;
  height: 32rpx;
  border-radius: 6rpx;
}

.cal-checked {
  background: #FFD700;
}

.cal-empty {
  background: rgba(255, 255, 255, 0.15);
}

.card-footer {
  text-align: center;
  margin-top: 16rpx;
}

.footer-text {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.4);
}

.actions {
  text-align: center;
}

.btn-save {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 48rpx;
  padding: 24rpx 80rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 6rpx 24rpx rgba(102, 126, 234, 0.4);
}

.save-hint {
  display: block;
  font-size: 22rpx;
  color: $text-muted;
  margin-top: 16rpx;
}
</style>
