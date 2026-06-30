<template>
  <view class="notebook">
    <view class="search-bar"><input class="search-input" v-model="searchText" placeholder="&#x1F50D; 搜索单词或卡片组..." /></view>
    <view class="filter-row">
      <view class="filter-tag" v-for="f in filters" :key="f.value" :class="{ active: activeFilter === f.value }" @click="activeFilter = f.value"><text>{{ f.label }}</text></view>
    </view>
    <view class="group-list" v-if="filteredGroups.length > 0">
      <view class="card-swipe-wrap" v-for="group in filteredGroups" :key="group.id">
        <view class="card-del-bg" @tap.stop="deleteGroup(group)"><text>&#x1F5D1; 删除</text></view>
        <view class="group-card" :class="{ 'swiped': swipedId === group.id }" @click="handleCardClick(group)" @touchstart="onTouchStart($event, group.id)" @touchend="onTouchEnd($event, group.id)">
          <view class="group-left"><text>&#x1F4D3;</text></view>
          <view class="group-body"><text class="group-title">{{ group.title || '未命名卡片组' }}</text><text class="group-source" v-if="group.source_text">{{ group.source_text.substring(0, 40) }}{{ group.source_text.length > 40 ? '...' : '' }}</text></view>
          <view class="group-right"><view class="group-status" :class="'gs-' + (group.group_status || 'pending')">{{ statusLabel(group.group_status) }}</view><text class="group-count">{{ group.card_count || 0 }} 词</text><text class="group-date">{{ formatDate(group.created_at) }}</text></view>
        </view>
      </view>
    </view>
    <view class="empty-state" v-if="filteredGroups.length === 0 && groups.length === 0">
      <text class="empty-icon">&#x1F4ED;</text>
      <text class="empty-text">学习本还是空的</text>
      <text class="empty-hint">快去首页拍照开始学习吧！</text>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref, computed } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { api } from "@/api";
const groups = ref<any[]>([]); const searchText = ref(""); const activeFilter = ref("all");
const swipedId = ref(""); const touchStartX = ref(0);
const filters = [{ label: "全部", value: "all" }, { label: "学习中", value: "learning" }, { label: "已学完", value: "learn_done" }, { label: "已通关", value: "test_done" }];
const filteredGroups = computed(() => { let list = groups.value; if (activeFilter.value !== "all") list = list.filter(g => g.group_status === activeFilter.value); if (searchText.value.trim()) { const kw = searchText.value.trim().toLowerCase(); list = list.filter(g => (g.title || "").toLowerCase().includes(kw) || (g.source_text || "").toLowerCase().includes(kw)); } return list; });
onShow(async () => { try { groups.value = await api.listCardGroups(true) || []; } catch (_e) { } });
function statusLabel(s: string): string { return { pending: "待学习", learning: "学习中", learn_done: "已学完", testing: "测验中", test_done: "已通关" }[s] || s || "待学习"; }
function formatDate(d: string) { if (!d) return ""; const date = new Date(d); return `${date.getMonth() + 1}/${date.getDate()}`; }
function onTouchStart(e: any, id: string) { touchStartX.value = e.touches[0].clientX; }
function onTouchEnd(e: any, id: string) { const dx = e.changedTouches[0].clientX - touchStartX.value; if (dx < -60) swipedId.value = id; else swipedId.value = ""; }
function handleCardClick(group: any) { if (swipedId.value) { swipedId.value = ""; return; } uni.navigateTo({ url: `/pages/card-detail/card-detail?groupId=${group.id}` }); }
async function deleteGroup(group: any) {
  uni.showModal({ title: "确认删除", content: `确定删除"${group.title || '未命名卡片组'}"？删除后不可恢复。`, success: async (res) => { if (res.confirm) { try { await api.deleteCardGroup(group.id); swipedId.value = ""; groups.value = groups.value.filter(g => g.id !== group.id); uni.showToast({ title: "已删除", icon: "success" }); } catch (_e) { uni.showToast({ title: "删除失败", icon: "none" }); } } } });
}
</script>
<style lang="scss" scoped>
.notebook { min-height: 100vh; background: $gradient-bg; }
.search-bar { padding: $spacing-md $spacing-xl; background: $bg-card; .search-input { background: $bg-primary; border-radius: $radius-pill; height: 72rpx; line-height: 72rpx; padding: 0 36rpx; font-size: $font-base; } }
.filter-row { display: flex; gap: $spacing-base; padding: $spacing-base $spacing-xl; background: $bg-card; border-bottom: 2rpx solid $border-light; .filter-tag { font-size: $font-sm; padding: 10rpx 24rpx; border-radius: $radius-pill; background: $bg-primary; color: $text-secondary; &.active { background: $bg-secondary; color: $primary; font-weight: $font-weight-semibold; } } }
.group-list { padding: $spacing-base $spacing-xl $spacing-xl; }
.card-swipe-wrap { position: relative; overflow: hidden; border-radius: $radius-md; margin-bottom: $spacing-sm; }
.card-del-bg { position: absolute; right: 0; top: 0; bottom: 0; width: 120rpx; background: $gradient-primary; border-radius: 0 $radius-md $radius-md 0; display: flex; align-items: center; justify-content: center; color: #fff; font-size: $font-base; font-weight: $font-weight-semibold; }
.group-card { position: relative; z-index: 1; display: flex; align-items: center; background: $bg-card; border-radius: $radius-md; padding: $spacing-lg; box-shadow: $shadow-sm; border: 2rpx solid $border-light; transition: transform 0.25s cubic-bezier(0.32, 0.72, 0, 1); &.swiped { transform: translateX(-120rpx); } &:active { transform: scale(0.98); } }
.group-left { width: 72rpx; height: 72rpx; border-radius: $radius-sm; background: $bg-secondary; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-right: $spacing-md; }
.group-body { flex: 1; overflow: hidden; margin-right: $spacing-base; .group-title { font-size: $font-lg; font-weight: $font-weight-semibold; color: $text-primary; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .group-source { font-size: $font-sm; color: $text-muted; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } }
.group-right { flex-shrink: 0; text-align: right; .group-status { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: $radius-pill; display: inline-block; margin-bottom: 6rpx; &.gs-pending { background: $bg-secondary; color: $text-secondary; } &.gs-learning { background: #FFF3BF; color: #E67700; } &.gs-learn_done { background: #E0E7FF; color: #6366F1; } &.gs-testing { background: #FFF0E8; color: #F97316; } &.gs-test_done { background: #D1FAE5; color: #059669; } } .group-count { font-size: $font-sm; font-weight: $font-weight-semibold; color: $primary; background: $bg-secondary; padding: 6rpx 16rpx; border-radius: $radius-pill; display: inline-block; } .group-date { font-size: 22rpx; color: $text-muted; display: block; } }
</style>
