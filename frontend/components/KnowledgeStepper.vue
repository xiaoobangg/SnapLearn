<template>
	<view class="ks-root">
		<view class="done" v-if="finished">
			<view class="done-header">
				<view class="done-icon-wrap"><text class="done-icon">&#x1F389;</text></view><text
					class="done-title">{{ completionTitle }}</text><text class="done-desc">{{ completionDesc }}</text>
			</view>
			<scroll-view scroll-y class="all-kp-scroll" :style="{ height: allKpScrollHeight + 'px' }">
				<view class="all-kp-list">
					<view class="all-kp-card" v-for="card in displayAllCards" :key="cardId(card)">
						<view class="all-kp-word">{{ card.word }}</view>
						<view class="kp-item" v-for="kp in (card.knowledgePoints || [])" :key="kp.id || kp.point_type" @click="speakKp(kp)">
							<view class="kp-item-header">
								<view class="kp-type-badge" :class="kp.pointType || kp.point_type">
									{{ kpTypeLabel(kp.pointType || kp.point_type) }}
								</view><text class="kp-speak">&#x1F50A;</text>
							</view>
							<text>{{ kp.content }}</text>
						</view>
					</view>
				</view>
			</scroll-view>
			<view class="bottom-bar" v-if="groupStatus === 'learn_done'">
				<view class="kp-btn confirm" @click="$emit('test')">&#x1F4DD; 开始测验</view>
			</view>
			<view class="bottom-bar" v-else>
				<view class="relearn-notice">
					<text class="relearn-count">&#x1F4CB; {{ relearnCards.size }} 个单词需要再学一轮</text>
				</view>
				<view class="kp-actions">
					<view class="kp-btn confirm" @click="startRelearn">&#x1F4D6; 继续学习</view>
				</view>
			</view>
		</view>
		<template v-else-if="cards.length > 0">
			<swiper class="card-swiper" :current="currentIndex" @change="onSwipeChange"
				:style="{ height: swiperHeight + 'px' }">
				<swiper-item v-for="(card, i) in cards" :key="cardId(card)">
					<view class="word-header" @click="speakWord(card.word)"><text class="card-word">{{ card.word }}</text><text class="speak-btn"
							>&#x1F50A;</text></view>
					<scroll-view scroll-y class="card-scroll" :style="{ height: scrollHeight + 'px' }">
						<view class="kp-list" v-if="(card.knowledgePoints || []).length > 0">
							<view class="kp-item" v-for="kp in card.knowledgePoints" :key="kp.id || kp.point_type" @click="speakKp(kp)">
								<view class="kp-item-header">
									<view class="kp-type-badge" :class="kp.pointType || kp.point_type">
										{{ kpTypeLabel(kp.pointType || kp.point_type) }}
									</view><text class="kp-speak">&#x1F50A;</text>
								</view>
								<text>{{ kp.content }}</text>
							</view>
						</view>
						<view class="kp-done-card" v-else><text>&#x2705;</text><text>已掌握</text><text>该单词所有知识点已确认</text>
						</view>
					</scroll-view>
				</swiper-item>
			</swiper>
			<view class="bottom-bar"
				v-if="currentCard && (currentCard.knowledgePoints||[]).length > 0 && currentCard.cardStatus !== 'mastered'">
				<view class="kp-actions">
					<view class="kp-btn relearn" @click="markRelearn(currentIndex)">&#x1F504; 需再学</view>
					<view class="kp-btn confirm" @click="markMastered(currentIndex)">&#x2705; 确认已学</view>
				</view>
			</view>
		</template>
	</view>
</template>
<script setup lang="ts">
	import { ref, computed, reactive, onMounted, watch, getCurrentInstance } from "vue"; import { api } from "@/api"; import { getApiBaseUrl, getServerBaseUrl } from "@/config";
	export interface CardLike { id : string; word : string; general_meaning ?: string; pronunciation ?: string; pos ?: string; knowledgePoints ?: KnowledgePointLike[];[key : string] : any; }
	export interface KnowledgePointLike { id : string; point_type : string; pointType ?: string; content : string; status : string; sort_order : number; }
	const props = withDefaults(defineProps<{ cards : CardLike[]; cardIdKey ?: string; completionTitle ?: string; completionDesc ?: string; allCards ?: CardLike[]; initialGroupStatus ?: string; }>(), { cardIdKey: "id", completionTitle: "学习完成！", completionDesc: "", allCards: () => [], initialGroupStatus: "" });
	const emit = defineEmits<{ back : []; mastered : [cardId: string]; relearn : [cardId: string]; test : []; relearnRound : [cardIds: string[]]; }>();
	const currentIndex = ref(0); const finished = ref(false); const swiperHeight = ref(600); const scrollHeight = ref(500); const allKpScrollHeight = ref(400);
	const masteredCards = reactive<Set<string>>(new Set()); const relearnCards = reactive<Set<string>>(new Set());
	const groupStatus = ref("");
	const currentCard = computed(() => props.cards[currentIndex.value] || null);
	const displayAllCards = computed(() => props.allCards.length > 0 ? props.allCards : props.cards);
	const processedCount = computed(() => masteredCards.size + relearnCards.size);
	// Stable dependency: watch card IDs string, not array reference — avoids reactivity loops
	const cardIdList = computed(() => props.cards.map(c => cardId(c)).join(','));
	function syncCardStatuses() {
		if (!props.cards || props.cards.length === 0) return;
		let changed = false;
		props.cards.forEach(c => {
			const s = (c as any).cardStatus || (c as any).card_status || "";
			if (s === "mastered") {
				const id = cardId(c);
				if (!masteredCards.has(id)) { masteredCards.add(id); changed = true; }
			}
		});
		if (changed) {
			// Double-guard: only set finished=true if (a) findNextUnprocessed returns -1
			// AND (b) no card has a data-level status of "relearn" or "unlearned".
			if (findNextUnprocessed(0) < 0) {
				const hasUnprocessed = props.cards.some(c => {
					const s = (c as any).cardStatus || (c as any).card_status || "";
					return s === "relearn" || s === "unlearned";
				});
				if (!hasUnprocessed) {
					finished.value = true;
					if (!groupStatus.value) groupStatus.value = props.initialGroupStatus || "";
				}
			}
		}
	}
	watch(() => props.initialGroupStatus, (s) => { if (s && finished.value && !groupStatus.value) groupStatus.value = s; });
	watch(cardIdList, () => syncCardStatuses(), { immediate: true });
	onMounted(() => { const sys = uni.getSystemInfoSync(); const instance = getCurrentInstance(); const query = uni.createSelectorQuery().in(instance); query.select('.ks-root').boundingClientRect((rect : any) => { if (rect && rect.height > 0) { swiperHeight.value = rect.height; scrollHeight.value = rect.height - 60; } else { swiperHeight.value = sys.windowHeight || 667; scrollHeight.value = (sys.windowHeight || 667) - 60; } }).exec(); allKpScrollHeight.value = (sys.windowHeight || 667) - 200; });
	function cardId(card : CardLike) : string { return card[props.cardIdKey] || ""; }
	function onSwipeChange(e : any) { currentIndex.value = e.detail.current; }
	function kpTypeLabel(type : string) : string { return ({ pronunciation: "发音", pos: "词性", general_meaning: "释义", extended_meaning: "延伸义", example_sentence: "例句", memory_tip: "记忆技巧" }[type] || type); }
	async function markMastered(i : number) { const card = props.cards[i]; if (!card) return; try { const res = await api.markCard(card.id, true); groupStatus.value = (res as any).group_status || ""; } catch (_e) { } masteredCards.add(cardId(card)); emit("mastered", cardId(card)); advanceCard(i); }
	async function markRelearn(i : number) { const card = props.cards[i]; if (!card) return; try { const res = await api.markCard(card.id, false); groupStatus.value = (res as any).group_status || ""; } catch (_e) { } relearnCards.add(cardId(card)); emit("relearn", cardId(card)); advanceCard(i); }
	function advanceCard(from : number) { const next = findNextUnprocessed(from); if (next >= 0) currentIndex.value = next; else finished.value = true; }
	function findNextUnprocessed(from : number) : number { for (let j = from + 1; j < props.cards.length; j++) { const id = cardId(props.cards[j]); if (!masteredCards.has(id) && !relearnCards.has(id)) return j; } for (let j = 0; j < from; j++) { const id = cardId(props.cards[j]); if (!masteredCards.has(id) && !relearnCards.has(id)) return j; } return -1; }
	function startRelearn() { const ids = Array.from(relearnCards); masteredCards.clear(); relearnCards.clear(); finished.value = false; currentIndex.value = 0; groupStatus.value = ""; emit("relearnRound", ids); }
	function playTtsUrl(url : string) { const a = uni.createInnerAudioContext(); a.src = url; a.play(); }
	function speakWord(word : string) { const t = uni.getStorageSync("access_token"); const c = currentCard.value?.id || ""; uni.request({ url: `${getApiBaseUrl()}/tts?text=${encodeURIComponent(word)}&cardId=${c}&type=word`, header: { Authorization: `Bearer ${t}` }, success: (r : any) => { if (r.data?.audio_url) playTtsUrl(getServerBaseUrl() + "/" + r.data.audio_url); } }); }
	function speakKp(kp : KnowledgePointLike) { if (!kp.content) return; const t = uni.getStorageSync("access_token"); const c = currentCard.value?.id || ""; uni.request({ url: `${getApiBaseUrl()}/tts?text=${encodeURIComponent(kp.content)}&cardId=${c}&type=${kp.point_type || kp.pointType}`, header: { Authorization: `Bearer ${t}` }, success: (r : any) => { if (r.data?.audio_url) playTtsUrl(getServerBaseUrl() + "/" + r.data.audio_url); } }); }
	defineExpose({ masteredCards, relearnCards, processedCount, currentIndex });
</script>
<style lang="scss" scoped>
	.ks-root { flex: 1; display: flex; flex-direction: column; background: $gradient-bg; }
	.done { display: flex; flex-direction: column; flex: 1; }
	.done-header { display: flex; flex-direction: column; align-items: center; padding: 48rpx 0 $spacing-xl; }
	.done-icon-wrap { width: 140rpx; height: 140rpx; border-radius: 50%; background: $gradient-primary; display: flex; align-items: center; justify-content: center; box-shadow: $shadow-sm; margin-bottom: $spacing-base;
		.done-icon { font-size: 64rpx; animation: bounce 1s ease infinite; } }
	.done-title { font-size: $font-xxl; font-weight: $font-weight-bold; color: $text-primary; }
	.done-desc { font-size: $font-base; color: $text-secondary; margin-top: $spacing-xs; }
	.all-kp-scroll { flex: 1; padding: 0 $spacing-xl 200rpx; }
	.all-kp-card { background: $bg-card; border-radius: $radius-md; padding: $spacing-xl; box-shadow: $shadow-sm; margin-bottom: $spacing-lg; }
	.all-kp-word { font-size: $font-hero; font-weight: $font-weight-extrabold; color: $text-primary; padding-bottom: $spacing-md; border-bottom: 2rpx solid $border-light; margin-bottom: $spacing-lg; }
	.card-swiper { width: 100%; }
	.card-scroll { padding: 0 $spacing-xl 200rpx; }
	.word-header { display: flex; align-items: center; justify-content: center; gap: $spacing-md; padding: $spacing-lg 0; background: $bg-card; flex-shrink: 0; z-index: 10; margin: 0 $spacing-xl; border-radius: $radius-md;
		.card-word { font-size: 56rpx; font-weight: $font-weight-extrabold; color: $text-primary; }
		.speak-btn { font-size: 44rpx; } }
	.kp-list { display: flex; flex-direction: column; gap: $spacing-md; padding-top: $spacing-base; }
	.kp-item { background: $bg-card; border-radius: $radius-md; padding: $spacing-xl; box-shadow: $shadow-sm; transition: $transition-base;
		&:active { transform: translateY(-2rpx); box-shadow: $shadow-md; } }
	.kp-item-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: $spacing-md; }
	.kp-type-badge { font-size: $font-sm; padding: 8rpx 24rpx; border-radius: $radius-pill; font-weight: $font-weight-semibold;
		&.pronunciation { color: #6366F1; background: #EEF2FF; }
		&.pos { color: #8B5CF6; background: #F5F3FF; }
		&.general_meaning { color: #059669; background: #D1FAE5; }
		&.extended_meaning { color: #D97706; background: #FEF3C7; }
		&.example_sentence { color: #DB2777; background: #FDF2F8; }
		&.memory_tip { color: #0891B2; background: #ECFEFF; } }
	.kp-speak { font-size: 36rpx; opacity: 0.5; padding: 8rpx; }
	.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; background: $bg-card; padding: $spacing-md $spacing-xl 40rpx; z-index: 100; box-shadow: 0 -4rpx 24rpx rgba(255,138,155,0.08); border-radius: $radius-xl $radius-xl 0 0; }
	.kp-actions { display: flex; gap: $spacing-lg; }
	.kp-btn { flex: 1; height: 104rpx; line-height: 104rpx; text-align: center; border-radius: $radius-pill; font-size: $font-lg; font-weight: $font-weight-semibold; transition: $transition-base;
		&:active { transform: scale(0.95); }
		&.confirm { background: $gradient-primary; color: #fff; box-shadow: $shadow-sm; }
		&.outline { background: $bg-card; color: $primary; border: 3rpx solid $primary; }
		&.relearn { background: #FEF3C7; color: #D97706; box-shadow: $shadow-yellow; } }
	.relearn-notice { text-align: center; padding-bottom: $spacing-md;
		.relearn-count { font-size: $font-base; color: #D97706; font-weight: $font-weight-semibold; } }
	.kp-done-card { background: #D1FAE5; border-radius: $radius-md; padding: 96rpx 40rpx; display: flex; flex-direction: column; align-items: center; box-shadow: $shadow-green; border: 3rpx solid #A7F3D0; font-size: $font-xl; font-weight: $font-weight-bold; color: #059669; margin-top: $spacing-base; }
	@keyframes bounce { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8rpx); } }
</style>