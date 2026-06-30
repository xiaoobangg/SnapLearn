<template>
  <view class="calendar-page">
    <view class="month-header"><text class="month-arrow" @click="prevMonth">&#x2039;</text><text class="month-title">{{ currentYear }}年 {{ currentMonth }}月</text><text class="month-arrow" @click="nextMonth">&#x203A;</text></view>
    <view class="weekday-row"><text class="weekday" v-for="d in weekdays" :key="d">{{ d }}</text></view>
    <view class="calendar-grid">
      <view class="day-cell" v-for="(day, di) in calendarDays" :key="'day-' + di">
        <template v-if="day"><text class="day-num" :class="{ today: day.isToday, checked: day.checked }">{{ day.date }}</text><view class="day-dot" v-if="day.checked" /></template>
      </view>
    </view>
    <view class="stats-section">
      <view class="stats-card">
        <view class="stat-row">
          <view class="stat-cell"><text class="sc-num">{{ stats.total_checkin_days || 0 }}</text><text class="sc-label">累计打卡</text></view>
          <view class="stat-cell"><text class="sc-num highlight">{{ stats.consecutive_days || 0 }}</text><text class="sc-label">连续天数</text></view>
          <view class="stat-cell"><text class="sc-num">{{ stats.total_pool_words || 0 }}</text><text class="sc-label">单词总量</text></view>
          <view class="stat-cell"><text class="sc-num success">{{ stats.mastered_count || 0 }}</text><text class="sc-label">已掌握</text></view>
        </view>
      </view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api";
const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
const currentYear = ref(new Date().getFullYear()); const currentMonth = ref(new Date().getMonth() + 1);
const checkinDays = ref<string[]>([]); const dayStats = ref<Record<string, { new_words: number; review_words: number }>>({});
const stats = ref({ total_checkin_days: 0, consecutive_days: 0, total_pool_words: 0, mastered_count: 0 });
const calendarDays = computed(() => {
  const year = currentYear.value; const month = currentMonth.value;
  const firstDay = new Date(year, month - 1, 1).getDay(); const daysInMonth = new Date(year, month, 0).getDate();
  const today = new Date(); const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,"0")}-${String(today.getDate()).padStart(2,"0")}`;
  const days: any[] = []; for (let i = 0; i < firstDay; i++) days.push(null);
  for (let d = 1; d <= daysInMonth; d++) { const s = `${year}-${String(month).padStart(2,"0")}-${String(d).padStart(2,"0")}`; days.push({ date: d, isToday: s === todayStr, checked: checkinDays.value.includes(s), stats: dayStats.value[s] }); }
  return days;
});
onLoad(() => { loadCalendar(); loadStats(); });
async function loadCalendar() { try { const data = await api.getCheckinCalendar(currentYear.value, currentMonth.value); checkinDays.value = (data as any).checkin_days || []; dayStats.value = (data as any).day_stats || {}; } catch (_e) { } }
async function loadStats() { try { stats.value = await api.getCheckinStats(); } catch (_e) { } }
function prevMonth() { if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value--; } else { currentMonth.value--; } loadCalendar(); }
function nextMonth() { if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++; } else { currentMonth.value++; } loadCalendar(); }
</script>
<style lang="scss" scoped>
.calendar-page { min-height: 100vh; background: $gradient-bg; padding: $spacing-lg $spacing-xl; }
.month-header { display: flex; align-items: center; justify-content: center; gap: $spacing-xxl; margin-bottom: $spacing-xl; .month-arrow { font-size: 48rpx; color: $primary; } .month-title { font-size: $font-lg; font-weight: $font-weight-bold; color: $text-primary; } }
.weekday-row { display: flex; margin-bottom: $spacing-sm; .weekday { flex: 1; text-align: center; font-size: $font-sm; color: $text-muted; } }
.calendar-grid { display: flex; flex-wrap: wrap; background: $bg-card; border-radius: $radius-md; padding: 8rpx; box-shadow: $shadow-sm; }
.day-cell { width: calc(100% / 7); aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; .day-num { font-size: $font-base; color: $text-primary; &.today { color: $primary; font-weight: $font-weight-bold; } &.checked { color: #fff; } } .day-dot { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 56rpx; height: 56rpx; border-radius: 50%; background: $gradient-primary; z-index: -1; } }
.stats-section { margin-top: $spacing-xl; }
.stats-card { background: $bg-card; border-radius: $radius-md; padding: $spacing-xl $spacing-lg; box-shadow: $shadow-sm; }
.stat-row { display: flex; justify-content: space-around; }
.stat-cell { display: flex; flex-direction: column; align-items: center; .sc-num { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; &.highlight { color: $warning; } &.success { color: $success; } } .sc-label { font-size: 22rpx; color: $text-secondary; margin-top: 6rpx; } }
</style>
