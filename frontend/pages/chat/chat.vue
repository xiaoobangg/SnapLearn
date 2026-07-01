<template>
	<view class="chat-page">
		<view class="chat-header">
			<view class="ch-info">
				<view class="ch-title-row">
					<text class="ch-title"></text>
					<view class="ch-actions">
						<view class="ch-mode" @click="newChat"><text>💭 新对话</text></view>
						<view class="ch-mode" @click="showConvList = true" style="background: #FFF0E8;"><text>📋 历史</text></view>
					</view>
				</view>
			</view>
		</view>
		<scroll-view scroll-y class="chat-body" :scroll-top="scrollTop" :scroll-with-animation="true">
			<view v-if="messages.length === 0" class="chat-empty">
				<view class="ce-avatar-wrap"><text>🤖</text></view>
				<text>💬 有什么英语学习问题？</text>
				<text>可以问我单词释义、语法规则、翻译等</text>
			</view>
			<view v-for="(msg, mi) in messages" :key="mi" class="chat-msg" :class="{ user: msg.role === 'user' }">
				<view class="cm-avatar" v-if="msg.role === 'assistant'"><text>🤖</text></view>
				<view class="cm-bubble"><text>{{ msg.content }}</text>
					<view class="cm-cursor" v-if="mi === messages.length - 1 && streaming">|</view>
				</view>
				<view class="cm-avatar" v-if="msg.role === 'user'"><text>😊</text></view>
			</view>
		</scroll-view>
		<view class="sheet-overlay" v-if="showConvList" @click="showConvList = false">
			<view class="sheet-panel" @click.stop>
				<view class="sheet-header"><text>📋 历史对话</text><text class="sheet-close" @click="showConvList = false">✕</text></view>
				<scroll-view scroll-y class="sheet-body">
					<view class="conv-item" v-for="conv in conversations" :key="conv.chat_id" @click="loadConversation(conv.chat_id)">
						<text class="conv-title">{{ conv.title || '新对话' }}</text>
						<text class="conv-del" @click.stop="deleteConversation(conv.chat_id)">🗑</text>
					</view>
					<view v-if="conversations.length === 0"><text>暂无历史对话</text></view>
				</scroll-view>
			</view>
		</view>
		<view class="chat-input-bar">
			<view class="ci-camera" @click="handleCamera"><text>📷</text></view>
			<textarea class="ci-input" v-model="inputText" placeholder="✍ 输入你的问题..." :disabled="streaming" :auto-height="true" :show-confirm-bar="false" :adjust-position="true" maxlength="-1" cursor-spacing="20" />
			<view class="ci-btn" :class="{ disabled: !inputText.trim() || streaming }" @click="sendMessage"><text v-if="!streaming">🚀 发送</text><text v-else>⏳</text></view>
		</view>
	</view>
</template>
<script setup>
	import { ref, nextTick } from "vue";
	import { onLoad } from "@dcloudio/uni-app";
	import { getApiBaseUrl } from "@/config";
	import { logger } from "@/utils/logger";

	const messages = ref([]);
	const inputText = ref("");
	const streaming = ref(false);
	const scrollTop = ref(0);
	const showConvList = ref(false);
	const conversations = ref([]);
	const currentChatId = ref("");
	const defaultModel = ref("deepseek");

	onLoad(() => { loadConversations(); loadSettings(); });

	function newChatId() { return Date.now().toString(36) + Math.random().toString(36).substring(2, 8); }
	function newChat() { messages.value = []; currentChatId.value = ""; logger.info("CHAT", "newChat"); }
	function updateMessageContent(index, content) { messages.value[index] = { ...messages.value[index], content: content }; }

	function decodeUtf8(bytes) {
		let result = "", i = 0;
		while (i < bytes.length) {
			const b1 = bytes[i++];
			if (b1 < 0x80) result += String.fromCharCode(b1);
			else if ((b1 & 0xE0) === 0xC0) { const b2 = bytes[i++]; result += String.fromCharCode(((b1 & 0x1F) << 6) | (b2 & 0x3F)); }
			else if ((b1 & 0xF0) === 0xE0) { const b2 = bytes[i++], b3 = bytes[i++]; result += String.fromCharCode(((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)); }
			else i += 3;
		}
		return result;
	}

	async function handleCamera() {
		const res = await uni.showActionSheet({ itemList: ["拍照", "从相册选择"] });
		if (res.tapIndex === undefined) return;
		const sourceType = res.tapIndex === 0 ? ["camera"] : ["album"];
		uni.chooseImage({ count: 1, sourceType, success: async (imgRes) => {
			const filePath = imgRes.tempFilePaths[0];
			uni.showLoading({ title: "识别中..." });
			const token = uni.getStorageSync("access_token");
			const doUpload = (url) => new Promise((resolve, reject) => {
				uni.uploadFile({ url, filePath, name: "image", header: { Authorization: "Bearer " + token }, success: resolve, fail: reject });
			});
			try {
				const res = await doUpload(`${getApiBaseUrl()}/ocr/recognize-ai`);
				logger.info("CHAT", "OCR upload res", { statusCode: res.statusCode, data: res.data });
				const data = JSON.parse(res.data);
				const words = data.words || [];
				uni.hideLoading();
				if (words.length > 0) {
					inputText.value = `请帮我创建一个包含这些单词的卡片组: ${words.join(", ")}`;
					logger.info("CHAT", "OCR done, words filled", { count: words.length });
				} else {
					uni.showToast({ title: "未识别到英文单词", icon: "none" });
				}
			} catch (_e) { uni.hideLoading(); logger.error("CHAT", "OCR upload failed", _e); uni.showToast({ title: "识别失败", icon: "none" }); }
		}});
	}

	async function sendMessage() {
		const text = inputText.value.trim();
		if (!text || streaming.value) return;
		const token = uni.getStorageSync("access_token");
		if (!token) { uni.showToast({ title: "请先登录", icon: "none" }); return; }

		messages.value.push({ role: "user", content: text });
		messages.value.push({ role: "assistant", content: "" });
		inputText.value = "";
		scrollToBottom();

		const BASE = getApiBaseUrl();
		const lastIdx = messages.value.length - 1;
		streaming.value = true;
		if (!currentChatId.value) currentChatId.value = newChatId();
		const body = { message: text, model: defaultModel.value, chat_id: currentChatId.value, mode: "agent" };
		logger.info("CHAT", "sendMessage", { text: text.substring(0, 50), model: defaultModel.value, chatId: currentChatId.value });

		let sseBuffer = "", chunkCount = 0;
		function processChunk(rawData) {
			sseBuffer += rawData;
			const events = sseBuffer.split("\n\n"); sseBuffer = events.pop() || "";
			for (const part of events) {
				const data = part.replace(/^data:\s*/, "").trim().replaceAll("data:", "");
				if (!data) continue;
				if (data === "[DONE]") { logger.info("CHAT", "stream done", { chunks: chunkCount, replyLen: messages.value[lastIdx].content.length }); streaming.value = false; return; }
				chunkCount++;
				if (chunkCount <= 3) logger.debug("CHAT", `chunk#${chunkCount}`, { len: data.length, preview: data.substring(0, 40) });
				updateMessageContent(lastIdx, messages.value[lastIdx].content + data); scrollToBottom();
			}
		}

		// #ifdef H5
		try {
			const response = await fetch(BASE + "/chat/stream", { method: "POST", headers: { "Content-Type": "application/json", Authorization: "Bearer " + token }, body: JSON.stringify(body) });
			if (!response.body) { const r = await response.json(); if (r.content) updateMessageContent(lastIdx, r.content); streaming.value = false; return; }
			const reader = response.body.getReader(); const decoder = new TextDecoder();
			while (true) { const { done, value } = await reader.read(); if (done) break; processChunk(decoder.decode(value, { stream: true })); }
		} catch (e) { logger.error("CHAT", "H5 fetch error", e); updateMessageContent(lastIdx, "请求失败"); }
		streaming.value = false;
		// #endif

		// #ifdef MP-WEIXIN
		const task = uni.request({ url: BASE + "/chat/stream", method: "POST", header: { "Content-Type": "application/json", Authorization: "Bearer " + token }, data: body, enableChunked: true, responseType: "arraybuffer",
			fail: (err) => { logger.error("CHAT", "MP request fail", err); updateMessageContent(lastIdx, "请求失败：" + (err.errMsg || "网络错误")); streaming.value = false; },
			complete: () => { streaming.value = false; }
		});
		task.onChunkReceived((res) => {
			let bytes = null;
			if (res.data instanceof ArrayBuffer) bytes = new Uint8Array(res.data);
			else if (res.data && res.data.data instanceof ArrayBuffer) bytes = new Uint8Array(res.data.data);
			if (bytes) processChunk(decodeUtf8(bytes));
		});
		// #endif
	}

	function scrollToBottom() { nextTick(() => { scrollTop.value = 999999; }); }

	async function loadSettings() {
		try {
			const res = await uni.request({ url: `${getApiBaseUrl()}/checkin/settings/chat`, header: { Authorization: "Bearer " + uni.getStorageSync("access_token") } });
			const data = (res).data || {};
			if (data.chat_model) { defaultModel.value = data.chat_model; logger.info("CHAT", "loaded model from settings", { model: defaultModel.value }); }
		} catch (_e) {}
	}

	async function loadConversations() {
		const token = uni.getStorageSync("access_token");
		const url = `${getApiBaseUrl()}/chat/conversations`;
		try {
			const res = await uni.request({ url, header: { Authorization: "Bearer " + token } });
			const list = (res).data || [];
			conversations.value = list;
			if (!currentChatId.value && list.length > 0) await loadConversation(list[0].chat_id);
		} catch (_e) {}
	}

	async function loadConversation(chatId) {
		currentChatId.value = chatId; showConvList.value = false;
		try {
			const res = await uni.request({ url: `${getApiBaseUrl()}/chat/messages/` + chatId + "?mode=agent", header: { Authorization: "Bearer " + uni.getStorageSync("access_token") } });
			const data = (res).data || [];
			messages.value = data.map((m) => ({ role: m.role, content: m.content }));
			scrollToBottom();
		} catch (_e) {}
	}

	async function deleteConversation(chatId) {
		const modalRes = await uni.showModal({ title: "删除对话", content: "确定删除该对话记录吗？删除后不可恢复。", confirmText: "删除", confirmColor: "#FF6B6B" });
		if (!modalRes.confirm) return;
		try {
			await uni.request({ url: `${getApiBaseUrl()}/chat/conversations/` + chatId, method: "DELETE", header: { Authorization: "Bearer " + uni.getStorageSync("access_token") } });
			conversations.value = conversations.value.filter(c => c.chat_id !== chatId);
			if (currentChatId.value === chatId) { currentChatId.value = ""; messages.value = []; }
			uni.showToast({ title: "已删除", icon: "success" });
		} catch (_e) { uni.showToast({ title: "删除失败", icon: "none" }); }
	}
</script>
<style lang="scss" scoped>
	.chat-page{height:100vh;background:$gradient-bg;display:flex;flex-direction:column;overflow:hidden}
	.chat-header{flex-shrink:0;padding:$spacing-md $spacing-xl;background:$bg-card;border-bottom:2rpx solid $border-light;.ch-info{display:flex;flex-direction:column;gap:6rpx;.ch-title-row{display:flex;align-items:center;justify-content:space-between}.ch-title{font-size:$font-lg;font-weight:$font-weight-bold}.ch-actions{display:flex;align-items:center;gap:$spacing-sm;.ch-mode{padding:6rpx 16rpx;border-radius:$radius-pill;background:$bg-secondary;color:$text-secondary;font-size:$font-sm}}}.ch-subtitle{font-size:22rpx;color:$text-muted}}
	.chat-body{flex:1;overflow-y:auto;padding:$spacing-lg $spacing-xl}
	.chat-empty{display:flex;flex-direction:column;align-items:center;padding-top:120rpx;color:$text-secondary;.ce-avatar-wrap{width:120rpx;height:120rpx;border-radius:50%;background:$bg-secondary;display:flex;align-items:center;justify-content:center;margin-bottom:$spacing-lg;animation:bounce 2s ease-in-out infinite;font-size:56rpx}}
	.chat-msg{display:flex;margin-bottom:$spacing-lg;&.user{flex-direction:row-reverse}}
	.cm-avatar{width:64rpx;height:64rpx;border-radius:50%;background:$bg-card;display:flex;align-items:center;justify-content:center;font-size:$font-lg;flex-shrink:0;box-shadow:$shadow-sm}
	.cm-bubble{max-width:70%;padding:$spacing-md $spacing-lg;border-radius:$radius-md;background:$bg-card;box-shadow:$shadow-sm;font-size:$font-base;line-height:1.6;white-space:pre-wrap;word-break:break-word}
	.user .cm-bubble{background:$gradient-primary;color:#fff}
	.cm-cursor{display:inline;color:#fff;animation:blink 0.8s infinite}
	@keyframes blink{50%{opacity:0}}
	.ci-camera{width:64rpx;height:64rpx;display:flex;align-items:center;justify-content:center;font-size:40rpx;background:$bg-primary;border-radius:50%;flex-shrink:0;align-self:flex-end}
	.chat-input-bar{flex-shrink:0;display:flex;align-items:flex-end;gap:$spacing-base;padding:$spacing-base $spacing-xl;background:$bg-card;border-top:2rpx solid $border-light;.ci-input{flex:1;min-height:72rpx;max-height:240rpx;background:$bg-primary;border-radius:$radius-md;padding:16rpx $spacing-lg;font-size:$font-base;line-height:1.5;box-sizing:border-box;overflow-y:auto}.ci-btn{padding:16rpx 32rpx;background:$gradient-primary;color:#fff;border-radius:$radius-pill;font-size:$font-base;box-shadow:$shadow-sm;flex-shrink:0;align-self:flex-end}}
	.conv-item{display:flex;align-items:center;padding:24rpx 0;border-bottom:2rpx solid $border-light;font-size:$font-base;&:active{background:$bg-primary}}
	.conv-title{flex:1}
	.conv-del{padding:8rpx 16rpx;font-size:28rpx;&:active{opacity:0.5}}
	.sheet-overlay{position:fixed;inset:0;background:$bg-overlay;backdrop-filter:blur(8rpx);display:flex;align-items:flex-end;justify-content:center;z-index:1000}
	.sheet-panel{background:$bg-card;border-radius:$radius-xl $radius-xl 0 0;width:100%;max-height:70vh;display:flex;flex-direction:column}
	.sheet-header{display:flex;align-items:center;justify-content:space-between;padding:$spacing-xl $spacing-xl $spacing-md;border-bottom:2rpx solid $border-light;font-size:$font-lg;font-weight:$font-weight-bold}.sheet-close{width:56rpx;height:56rpx;line-height:56rpx;text-align:center;border-radius:50%;background:$bg-secondary;font-size:28rpx;color:$text-muted}
	.sheet-body{flex:1;overflow-y:auto;padding:$spacing-base $spacing-xl}
</style>
