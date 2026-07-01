<template>
	<view class="card-detail">
		<view class="top-bar">
			<view class="progress-wrap" v-if="stepperRef">
				<view class="progress-bar">
					<view v-for="(card, ci) in cardsWithKps" :key="ci" class="pb-seg" :class="segClass(card, ci)" :style="{ width: (100/cardsWithKps.length) + '%' }" />
				</view>
				<text class="progress-text">{{ displayProgress }} / {{ cards.length }}</text>
			</view>
			<text class="menu-btn" @click="openMoveSheet" v-if="currentCard">&#x22EF;</text>
		</view>
		<view class="status-banner done" v-if="learnStatus==='learn_done'" @click="goTest"><text>&#x2705; 学习完成，点击开始测验 &#x27A1;</text></view>
		<view class="status-banner testing" v-else-if="learnStatus==='testing'"><text>&#x1F4DD; 测验进行中</text></view>
		<view class="status-banner passed" v-else-if="learnStatus==='test_done'"><text>&#x1F389; 本组已通关！</text></view>
		<view class="relearn-banner" v-if="relearnCount>0 && !stepperFinished"><text>{{ relearnCount }} 个单词需再学</text></view>
		<KnowledgeStepper v-if="learnStatus!=='test_done'" ref="stepperRef" :key="stepperKey"
			:cards="stepperCards" :all-cards="cardsWithAllKps" card-id-key="id"
			completion-title="&#x1F389; 本组学习完成！"
			:completion-desc="relearnCount>0 ? `${relearnCount} 个单词需要再学一轮` : '所有单词已掌握，可以开始测验了'"
			:initial-group-status="learnStatus"
			@mastered="onCardMastered" @relearn="onCardRelearn" @test="goTest" @relearn-round="onRelearnRound" />
		<view class="pass-done" v-else>
			<view class="pd-icon-wrap"><text>&#x1F389;</text></view>
			<text class="pd-title">恭喜通关！</text>
			<text class="pd-desc">本组所有单词已掌握并通过测验</text>
			<button class="btn-back-home" @click="goBack">&#x1F3E0; 返回首页</button>
		</view>
		<view class="sheet-overlay" v-if="showMoveSheet" @click="showMoveSheet=false">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-header"><text class="sheet-title">移动「{{ currentCard?.word }}」</text><text class="sheet-close" @click="showMoveSheet=false">&#x2715;</text></view>
				<view class="sheet-new"><input class="new-input" v-model="newGroupTitle" placeholder="&#x2795; 新建卡片组名称" /><button class="btn-new" @click="moveToNewGroup" :disabled="!newGroupTitle.trim()">&#x2795; 新建并移动</button></view>
				<view class="sheet-divider"><text>或移到已有卡片组</text></view>
				<scroll-view scroll-y class="sheet-body" v-if="otherGroups.length>0">
					<view class="sheet-item" v-for="g in otherGroups" :key="g.id" @click="moveToGroup(g.id)">
						<view class="si-left"><text>&#x1F4D3;</text></view>
						<view class="si-body"><text>{{ g.title || '未命名卡片组' }}</text><text>{{ g.card_count || 0 }} 词</text></view><text>&#x27A1;</text>
					</view>
				</scroll-view>
			</view>
		</view>
	</view>
</template>
<script setup lang="ts">
	import { ref, computed, reactive } from "vue"; import { onLoad } from "@dcloudio/uni-app"; import { api } from "@/api"; import type { CardResponse } from "@/api/types"; import KnowledgeStepper from "@/components/KnowledgeStepper.vue";
	const groupId = ref(""); const groupTitle = ref(""); const cards = ref<CardResponse[]>([]); const learnStatus = ref(""); const stepperRef = ref<InstanceType<typeof KnowledgeStepper>>();
	const showMoveSheet = ref(false); const newGroupTitle = ref(""); const otherGroups = ref<any[]>([]);
	// Parent-managed sets — track user actions independently from stepper internals
	const parentMasteredCards = reactive<Set<string>>(new Set());
	const relearnCardIds = reactive<Set<string>>(new Set());
	const stepperKey = ref(0);
	const displayProgress = computed(() => cardsWithKps.value.filter((c: any) => c.cardStatus !== "unlearned").length);
	const curIdx = computed(() => stepperRef.value?.currentIndex ?? 0);
	const stepperFinished = computed(() => stepperRef.value ? displayProgress.value >= cards.value.length : false);
	// cardsWithKps reads parent-managed Sets — no circular dependency on stepper internals
	const cardsWithKps = computed(() => {
		const list = cards.value.map(c => {
			let s = (c as any).cardStatus || (c as any).card_status || "unlearned";
			if (parentMasteredCards.has(c.id)) s = "mastered";
			if (relearnCardIds.has(c.id)) s = "relearn";
			return { ...c, cardStatus: s, knowledgePoints: (c as any).knowledgePoints || (c as any).knowledge_points || [] };
		});
		return list;
	});
	// Always pass ALL cards to the stepper — it handles which ones need interaction internally.
	// (Previously filtered to only relearn cards during relearn round, hiding mastered ones.)
	const stepperCards = computed(() => cardsWithKps.value);
	const cardsWithAllKps = computed(() => cards.value.map(c => ({ ...c, knowledgePoints: (c as any).allKnowledgePoints || (c as any).knowledgePoints || (c as any).knowledge_points || [] })));
	const relearnCount = computed(() => stepperRef.value?.relearnCards?.size || 0);
	const currentCard = computed(() => cardsWithKps.value[curIdx.value] || null);
	onLoad(async (o : any) => { groupId.value = o?.groupId || ""; if (groupId.value) { await loadGroup(); await loadLearnStatus(); } });
	async function loadGroup() { try { const g = await api.getCardGroup(groupId.value); cards.value = g.cards || []; groupTitle.value = (g as any).title || ""; } catch (_e) { uni.showToast({ title: "加载失败", icon: "none" }); } }
	async function loadLearnStatus() { try { const s = await api.getLearnStatus(groupId.value); learnStatus.value = (s as any).group_status || ""; } catch (_e) { } }
	function onCardMastered(cid: string) { parentMasteredCards.add(cid); }
	function onCardRelearn(cid: string) { relearnCardIds.add(cid); }
	// "继续学习" now behaves like reloading the page: re-fetch from API, clear local state, recreate stepper
	async function onRelearnRound(_cardIds: string[]) {
		relearnCardIds.clear();
		parentMasteredCards.clear();
		await loadGroup();
		await loadLearnStatus();
		stepperKey.value++;
	}
	async function openMoveSheet() { newGroupTitle.value = ""; showMoveSheet.value = true; try { const a = await api.listCardGroups(true); otherGroups.value = (a || []).filter((g : any) => g.id !== groupId.value); } catch (_e) { otherGroups.value = []; } }
	async function moveToGroup(tid : string) { if (!currentCard.value) return; try { await api.moveCard(currentCard.value.id, tid); uni.showToast({ title: "已移动", icon: "success" }); removeCardFromList(); } catch (_e) { uni.showToast({ title: "移动失败", icon: "none" }); } }
	async function moveToNewGroup() { if (!currentCard.value || !newGroupTitle.value.trim()) return; try { await api.moveCard(currentCard.value.id, undefined, newGroupTitle.value.trim()); uni.showToast({ title: "已新建并移动", icon: "success" }); removeCardFromList(); } catch (_e) { uni.showToast({ title: "移动失败", icon: "none" }); } }
	function removeCardFromList() { const c = currentCard.value; showMoveSheet.value = false; if (!c) return; cards.value = cards.value.filter(c2 => c2.id !== c.id); if (cards.value.length === 0) goBack(); }
	function segClass(card: any, idx: number) {
		let cls = "seg-pending";
		if (card.cardStatus === "mastered") cls = "seg-done";
		if (card.cardStatus === "relearn") cls = "seg-relearn";
		if (idx === curIdx.value) cls += " seg-current";
		return cls;
	}
	function goTest() { uni.navigateTo({ url: `/pages/test/test?groupId=${groupId.value}&groupName=${encodeURIComponent(groupTitle.value||"卡片组")}` }); }
	function goBack() { uni.switchTab({ url: "/pages/index/index" }); }
</script>
<style lang="scss" scoped>
	.top-bar { display: flex; align-items: center; padding: $spacing-lg $spacing-xl $spacing-base; gap: $spacing-base;
		.back-btn { color: $primary; font-size: $font-base; background: $bg-card; padding: 12rpx 24rpx; border-radius: $radius-pill; box-shadow: $shadow-sm; }
		.progress-text { font-size: $font-base; color: $text-primary; font-weight: $font-weight-bold; flex-shrink: 0; }
		.progress-wrap { flex: 1; display: flex; align-items: center; gap: 10rpx; }
		.progress-bar { flex: 1; height: 28rpx; background: #F3F4F6; border-radius: 14rpx; overflow: visible; position: relative; display: flex; }
		.pb-seg { height: 100%; position: relative; transition: background 0.3s; &:first-child { border-radius: 14rpx 0 0 14rpx; } &:last-child { border-radius: 0 14rpx 14rpx 0; } &.seg-current { &::after { content: "😊"; position: absolute; top: -25rpx; left: 50%; transform: translateX(-50%); font-size: 45rpx; } } &.seg-done { background: #10B981; } &.seg-relearn { background: #F59E0B; } &.seg-pending { background: #E5E7EB; } }
		.menu-btn { background: $bg-card; width: 56rpx; height: 56rpx; line-height: 56rpx; text-align: center; border-radius: 50%; box-shadow: $shadow-sm; } }
	.status-banner { margin: 0 $spacing-xl $spacing-sm; border-radius: $radius-md; padding: 18rpx $spacing-lg; font-size: $font-sm; font-weight: $font-weight-semibold;
		&.done { background: #D1FAE5; color: #059669; }
		&.testing { background: #FFF0E8; color: #F97316; }
		&.passed { background: #FEF3C7; color: #D97706; } }
	.relearn-banner { margin: 0 $spacing-xl $spacing-sm; background: #FEF3C7; border-radius: $radius-md; padding: 18rpx $spacing-lg; font-size: $font-sm; color: #D97706; }
	.pass-done { display: flex; flex-direction: column; align-items: center; padding-top: 120rpx;
		.pd-icon-wrap { width: 160rpx; height: 160rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; margin-bottom: $spacing-lg; font-size: 72rpx; }
		.pd-title { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; }
		.pd-desc { font-size: $font-base; color: $text-secondary; }
		.btn-back-home { margin-top: 56rpx; background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 24rpx 80rpx; font-size: $font-lg; font-weight: $font-weight-semibold; box-shadow: $shadow-sm; } }
	.sheet-new { display: flex; align-items: center; gap: $spacing-base; padding: $spacing-lg $spacing-xl;
		.new-input { flex: 1; height: 80rpx; background: $bg-primary; border-radius: $radius-pill; padding: 0 $spacing-lg; font-size: $font-base; }
		.btn-new { background: $gradient-primary; color: #fff; border: none; border-radius: $radius-pill; padding: 20rpx 32rpx; font-size: $font-base; font-weight: $font-weight-semibold; } }
	.sheet-divider { display: flex; align-items: center; padding: 8rpx $spacing-xl $spacing-base; color: $text-muted; }
	.sheet-item { display: flex; align-items: center; padding: 22rpx $spacing-xl;
		&:active { background: $bg-primary; }
		.si-left { width: 56rpx; height: 56rpx; border-radius: $radius-sm; background: $bg-secondary; display: flex; align-items: center; justify-content: center; margin-right: $spacing-base; }
		.si-body { flex: 1; } }
</style>