<template>
  <view class="share-page">
    <!-- Canvas 即预览，所见即所得 -->
    <canvas canvas-id="shareCanvas" class="share-canvas"
            :style="{ width: canvasW + 'px', height: canvasH + 'px' }" />

    <view class="actions">
      <button class="btn-save" @click="saveImage" :loading="saving">
        {{ saving ? '生成中...' : '保存图片' }}
      </button>
      <text class="save-hint">保存后可分享给好友</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "@/api";

const stats = ref({ consecutive_days: 0, total_checkin_days: 0, total_pool_words: 0, mastered_count: 0 });
const calendarDays = ref<{ day: number; checked: boolean; weekend: boolean }[]>([]);
const currentMonth = ref("");
const todayDate = ref("");
const saving = ref(false);
const canvasW = 375;
const canvasH = 620;
let ctx: any = null;

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

  // 初始化绘制
  setTimeout(() => { ctx = uni.createCanvasContext('shareCanvas'); drawCard(); }, 300);
});

function drawCard() {
  if (!ctx) return;
  const w = canvasW, h = canvasH;

  // Gradient background
  const grad = ctx.createLinearGradient(0, 0, w, h);
  grad.addColorStop(0, '#6366F1');
  grad.addColorStop(0.5, '#8B5CF6');
  grad.addColorStop(1, '#EC4899');
  ctx.setFillStyle(grad);
  ctx.fillRect(0, 0, w, h);

  // Inner card
  ctx.setFillStyle('rgba(255,255,255,0.12)');
  ctx.fillRect(16, 16, w - 32, h - 32);

  // Title
  ctx.setFillStyle('#ffffff');
  ctx.setFontSize(26);
  ctx.setTextAlign('center');
  ctx.fillText('拍立学', w / 2, 60);
  ctx.setFontSize(13);
  ctx.setFillStyle('rgba(255,255,255,0.6)');
  ctx.fillText('每日打卡', w / 2, 82);

  // Streak — side by side
  const sx = w / 2 - 55, sx2 = w / 2 + 55;
  ctx.setFillStyle('#FFD700');
  ctx.setFontSize(52);
  ctx.setShadow(0, 4, 12, 'rgba(0,0,0,0.25)');
  ctx.fillText(String(stats.value.consecutive_days), sx, 170);
  ctx.setShadow(0, 0, 0, 'transparent');
  ctx.setFontSize(12);
  ctx.setFillStyle('rgba(255,255,255,0.85)');
  ctx.fillText('连续打卡', sx, 190);

  // Divider
  ctx.setStrokeStyle('rgba(255,255,255,0.2)');
  ctx.setLineWidth(1);
  ctx.moveTo(w / 2, 145);
  ctx.lineTo(w / 2, 200);
  ctx.stroke();

  // Right side
  ctx.setFillStyle('rgba(255,255,255,0.85)');
  ctx.setFontSize(52);
  ctx.fillText(String(stats.value.total_checkin_days), sx2, 170);
  ctx.setFontSize(12);
  ctx.fillText('累计打卡', sx2, 190);

  // Month
  ctx.setFontSize(13);
  ctx.setFillStyle('rgba(255,255,255,0.7)');
  ctx.fillText(currentMonth.value, w / 2, 245);

  // Weekday headers
  const wds = ['日', '一', '二', '三', '四', '五', '六'];
  const cx = 30, cy = 270, cell = 40, gap = 5;
  ctx.setFontSize(10);
  wds.forEach((wd, i) => {
    ctx.setFillStyle('rgba(255,255,255,0.45)');
    ctx.fillText(wd, cx + i * (cell + gap) + cell / 2, cy);
  });

  // Day cells
  ctx.setFontSize(11);
  calendarDays.value.forEach((d, i) => {
    const col = i % 7;
    const row = Math.floor(i / 7);
    const x = cx + col * (cell + gap);
    const y = cy + 10 + row * (cell + gap);
    if (d.checked) {
      ctx.setFillStyle('#FFD700');
      ctx.fillRect(x, y, cell, cell);
      ctx.setFillStyle('#333');
    } else {
      ctx.setFillStyle(d.weekend ? 'rgba(255,255,255,0.2)' : 'rgba(255,255,255,0.5)');
    }
    if (d.day > 0) ctx.fillText(String(d.day), x + cell / 2, y + cell / 2 + 4);
  });

  // Footer
  ctx.setFontSize(11);
  ctx.setFillStyle('rgba(255,255,255,0.4)');
  ctx.setTextAlign('center');
  ctx.fillText(todayDate.value, w / 2, h - 25);

  ctx.draw();
}

async function saveImage() {
  saving.value = true;
  drawCard();
  setTimeout(() => {
    uni.canvasToTempFilePath({
      canvasId: 'shareCanvas',
      success: (res: any) => {
        uni.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
          fail: () => uni.showToast({ title: '保存失败', icon: 'none' }),
        });
      },
      fail: () => uni.showToast({ title: '生成失败', icon: 'none' }),
      complete: () => { saving.value = false; },
    });
  }, 300);
}
</script>

<script lang="ts">
export default {
  onShareAppMessage() {
    return { title: '拍立学 - 坚持打卡学英语，快来一起进步！', path: '/pages/index/index' };
  },
};
</script>

<style lang="scss" scoped>
.share-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
  min-height: 100vh;
  background: $gradient-bg;
}
.share-canvas {
  width: 630rpx;
  height: 1040rpx;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 40rpx rgba(99, 102, 241, 0.35);
  margin-bottom: 40rpx;
}
.actions { text-align: center; }
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
.save-hint { display: block; font-size: 22rpx; color: $text-muted; margin-top: 16rpx; }
</style>
