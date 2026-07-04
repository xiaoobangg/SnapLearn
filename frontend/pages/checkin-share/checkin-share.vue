<template>
  <view class="share-page">
    <view class="preview-card" id="previewCard">
      <view class="card-bg">
        <view class="card-inner">
          <view class="card-header">
            <view class="logo-wrap"><text class="logo-icon">📸</text></view>
            <text class="card-logo">拍立学</text>
            <text class="card-subtitle">拍立学 · 每日打卡</text>
          </view>

          <view class="streak-section">
            <text class="streak-icon">🔥</text>
            <text class="streak-number">{{ stats.consecutive_days }}</text>
            <text class="streak-label">连续打卡天数</text>
          </view>

          <view class="divider" />

          <view class="stats-row">
            <view class="stat-item">
              <text class="stat-icon">📅</text>
              <text class="stat-num">{{ stats.total_checkin_days }}</text>
              <text class="stat-text">累计打卡</text>
            </view>
            <view class="stat-item">
              <text class="stat-icon">📚</text>
              <text class="stat-num">{{ stats.total_pool_words }}</text>
              <text class="stat-text">单词总数</text>
            </view>
            <view class="stat-item">
              <text class="stat-icon">✅</text>
              <text class="stat-num">{{ stats.mastered_count }}</text>
              <text class="stat-text">已掌握</text>
            </view>
          </view>

          <view class="calendar-section" v-if="calendarDays.length > 0">
            <text class="cal-title">{{ currentMonth }}</text>
            <view class="cal-weekdays">
              <text v-for="wd in weekdays" :key="wd" class="cal-wd">{{ wd }}</text>
            </view>
            <view class="cal-grid">
              <view v-for="(d, i) in calendarDays" :key="i"
                    :class="['cal-day', d.checked ? 'cal-checked' : 'cal-empty', d.weekend ? 'cal-weekend' : '']">
                <text class="cal-day-num">{{ d.day || '' }}</text>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <text class="footer-text">{{ todayDate }}</text>
            <text class="footer-brand">扫码加入学习 →</text>
          </view>
        </view>
      </view>
    </view>

    <view class="actions">
      <button class="btn-save" @click="saveImage" :loading="saving">
        {{ saving ? '生成中...' : '保存图片' }}
      </button>
      <text class="save-hint">保存后可分享给好友</text>
    </view>

    <canvas canvas-id="shareCanvas"
            :style="{ width: canvasW + 'px', height: canvasH + 'px', position: 'fixed', left: '-9999px', top: 0 }" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "@/api";

const stats = ref({ consecutive_days: 0, total_checkin_days: 0, total_pool_words: 0, mastered_count: 0 });
const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
const calendarDays = ref<{ day: number; checked: boolean; weekend: boolean }[]>([]);
const currentMonth = ref("");
const todayDate = ref("");
const saving = ref(false);
const canvasW = 375 * 2;
const canvasH = 560 * 2;

onMounted(async () => {
  const now = new Date();
  todayDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;
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

    const checkinDays = new Set(cal.checkin_days || []);
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).getDay();
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const grid: { day: number; checked: boolean; weekend: boolean }[] = [];
    for (let i = 0; i < firstDay; i++) grid.push({ day: 0, checked: false, weekend: false });
    for (let d = 1; d <= daysInMonth; d++) {
      const ds = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const dow = new Date(now.getFullYear(), now.getMonth(), d).getDay();
      grid.push({ day: d, checked: checkinDays.has(ds), weekend: dow === 0 || dow === 6 });
    }
    calendarDays.value = grid;
  } catch (_) { /* ignore */ }
});

async function saveImage() {
  saving.value = true;
  try {
    // #ifdef MP-WEIXIN
    const ctx = uni.createCanvasContext('shareCanvas');
	    ctx.scale(2, 2);
    const w = 375;
    const h = 560;

    const gradient = ctx.createLinearGradient(0, 0, w, h);
    gradient.addColorStop(0, '#6366F1');
    gradient.addColorStop(0.5, '#8B5CF6');
    gradient.addColorStop(1, '#EC4899');
    ctx.setFillStyle(gradient);
    ctx.fillRect(0, 0, w, h);

    ctx.setFillStyle('rgba(255,255,255,0.15)');
    ctx.fillRect(16, 16, w - 32, h - 32);

    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(24);
    ctx.setTextAlign('center');
    ctx.fillText('📸', w / 2, 60);

    ctx.setFontSize(28);
    ctx.setFillStyle('#ffffff');
    ctx.fillText('拍立学', w / 2, 90);
    ctx.setFontSize(14);
    ctx.setFillStyle('rgba(255,255,255,0.7)');
    ctx.fillText('拍立学 · 每日打卡', w / 2, 112);

    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(14);
    ctx.fillText('🔥', w / 2, 155);

    ctx.setFillStyle('#FFD700');
    ctx.setFontSize(72);
    ctx.setTextAlign('center');
    ctx.setShadow(0, 4, 12, 'rgba(0,0,0,0.25)');
    const streak = String(stats.value.consecutive_days);
    ctx.fillText(streak, w / 2, 210);
    ctx.setShadow(0, 0, 0, 'transparent');

    ctx.setFontSize(16);
    ctx.setFillStyle('rgba(255,255,255,0.85)');
    ctx.fillText('连续打卡天数', w / 2, 238);

    ctx.setStrokeStyle('rgba(255,255,255,0.3)');
    ctx.setLineWidth(1);
    ctx.moveTo(50, 260);
    ctx.lineTo(w - 50, 260);
    ctx.stroke();

    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(30);
    ctx.setTextAlign('center');
    ctx.fillText('📅', w / 4, 300);
    ctx.fillText('📚', w / 2, 300);
    ctx.fillText('✅', w * 3 / 4, 300);

    ctx.setFontSize(28);
    ctx.fillText(String(stats.value.total_checkin_days), w / 4, 330);
    ctx.fillText(String(stats.value.total_pool_words), w / 2, 330);
    ctx.fillText(String(stats.value.mastered_count), w * 3 / 4, 330);

    ctx.setFontSize(12);
    ctx.setFillStyle('rgba(255,255,255,0.7)');
    ctx.fillText('累计打卡', w / 4, 352);
    ctx.fillText('单词总数', w / 2, 352);
    ctx.fillText('已掌握', w * 3 / 4, 352);

    ctx.setFillStyle('rgba(255,255,255,0.8)');
    ctx.setFontSize(14);
    ctx.fillText(currentMonth.value, w / 2, 395);

    // Weekday headers
    const wds = ['日','一','二','三','四','五','六'];
    const cx = 30, cy = 418, cell = 40, gap = 5;
    ctx.setFontSize(10);
    ctx.setTextAlign('center');
    wds.forEach((wd, i) => {
      ctx.setFillStyle('rgba(255,255,255,0.5)');
      ctx.fillText(wd, cx + i * (cell + gap) + cell / 2, cy - 18);
    });
    // Calendar days
    ctx.setFontSize(12);
    calendarDays.value.forEach((d, i) => {
      const col = i % 7;
      const row = Math.floor(i / 7);
      const x = cx + col * (cell + gap);
      const y = cy + row * (cell + gap);
      if (d.checked) {
        ctx.setFillStyle('#FFD700');
        ctx.fillRect(x, y, cell, cell);
        ctx.setFillStyle('#333');
      } else {
        ctx.setFillStyle(d.weekend ? 'rgba(255,255,255,0.25)' : 'rgba(255,255,255,0.6)');
      }
      if (d.day > 0) ctx.fillText(String(d.day), x + cell / 2, y + cell / 2 + 4);
    });

    ctx.setFillStyle('rgba(255,255,255,0.5)');
    ctx.setFontSize(11);
    ctx.setTextAlign('center');
    ctx.fillText(todayDate.value, w / 2, h - 50);
    ctx.setFontSize(12);
    ctx.setFillStyle('rgba(255,255,255,0.7)');
    ctx.fillText('扫码加入学习 →', w / 2, h - 28);

    ctx.draw(false, () => {
      uni.canvasToTempFilePath({
        canvasId: 'shareCanvas',
        fileType: 'png',
        quality: 1.0,
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
<script lang="ts">
export default {
  onShareAppMessage() {
    return {
      title: '拍立学 - 坚持打卡学英语，快来一起进步！',
      path: '/pages/index/index'
    };
  }
};
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
  box-shadow: 0 8rpx 40rpx rgba(99, 102, 241, 0.35);
  margin-bottom: 40rpx;
}
.card-bg { border-radius: 24rpx; overflow: hidden; }

.card-bg {
  background: linear-gradient(135deg, #6366F1, #8B5CF6, #EC4899);
  padding: 8rpx;
}

.card-inner {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 18rpx;
  padding: 48rpx 36rpx 40rpx;
}

.card-header {
  text-align: center;
  margin-bottom: 28rpx;
}

.logo-wrap {
  margin-bottom: 8rpx;
}

.logo-icon {
  font-size: 48rpx;
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

.streak-icon {
  display: block;
  font-size: 32rpx;
  margin-bottom: 8rpx;
}

.streak-number {
  font-size: 120rpx;
  font-weight: 900;
  color: #FFD700;
  line-height: 1;
  text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.25);
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

.stat-icon {
  display: block;
  font-size: 28rpx;
  margin-bottom: 6rpx;
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

.cal-weekdays { display: grid; grid-template-columns: repeat(7, 56rpx); margin: 0 auto 4rpx; width: 392rpx; }
.cal-wd { text-align: center; font-size: 18rpx; color: rgba(255,255,255,0.5); width: 56rpx; height: 28rpx; display: flex; align-items: center; justify-content: center; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 56rpx); margin: 0 auto; width: 392rpx; }
.cal-day { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; }
.cal-day-num { font-size: 20rpx; color: rgba(255,255,255,0.6); }
.cal-day.cal-checked { background: #FFD700; border-radius: 6rpx; }
.cal-day.cal-checked .cal-day-num { color: #333; font-weight: bold; }
.cal-day.cal-empty { background: transparent; }
.cal-day.cal-weekend.cal-empty .cal-day-num { color: rgba(255,255,255,0.25); }

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
  display: block;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8rpx;
}

.footer-brand {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

.actions {
  text-align: center;
}

.btn-save {
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: #fff;
  border: none;
  border-radius: 48rpx;
  padding: 24rpx 80rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 6rpx 24rpx rgba(99, 102, 241, 0.4);
}

.save-hint {
  display: block;
  font-size: 22rpx;
  color: $text-muted;
  margin-top: 16rpx;
}
</style>