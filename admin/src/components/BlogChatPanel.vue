<template>
  <div class="chat-panel" :class="{ collapsed: !showChat }">
    <div class="panel-head">
      <h4>AI 助手</h4>
      <el-button size="small" text class="panel-toggle" @click="showChat = !showChat">
        <el-icon :size="16"><DArrowRight v-if="showChat" /><DArrowLeft v-else /></el-icon>
      </el-button>
      <span class="chat-hint">基于博客文章回答</span>
    </div>
    <div class="chat-messages" ref="chatMsgsRef">
      <div v-for="(msg, i) in store.messages" :key="i" :class="['chat-msg', msg.role]">
        <div class="msg-content">{{ msg.content }}</div>
      </div>
      <div v-if="store.loading" class="chat-msg assistant"><div class="msg-content typing">...</div></div>
    </div>
    <div class="chat-input">
      <el-input v-model="store.input" size="small" placeholder="询问文章内容..." @keyup.enter="store.send" :disabled="store.loading" />
      <el-button size="small" type="primary" @click="store.send" :loading="store.loading">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { DArrowRight, DArrowLeft } from "@element-plus/icons-vue";
import { useBlogChatStore } from "@/store/blogChat";

const store = useBlogChatStore();
const showChat = ref(true);
const chatMsgsRef = ref<HTMLElement>();

onMounted(() => {
  store.setScrollRef(chatMsgsRef.value || null);
});
</script>

<style lang="scss" scoped>
.chat-panel {
  width: 320px; min-width: 280px;
  background: #fff;
  border-left: 1px solid #E5E7EB;
  display: flex; flex-direction: column; overflow: hidden;
  transition: width 0.25s ease, min-width 0.25s ease;

  &.collapsed {
    width: 36px; min-width: 36px;
    .chat-messages, .chat-input, .chat-hint { display: none; }
    .panel-head { padding: 12px 8px; h4 { display: none; } }
  }

  .panel-head {
    position: relative;
    padding: 14px 16px; border-bottom: 1px solid #E5E7EB;
    h4 { margin: 0; font-size: 15px; font-weight: 600; color: #303133; }
    .chat-hint { font-size: 11px; color: #909399; display: block; margin-top: 2px; }
    .panel-toggle { position: absolute; right: 4px; top: 8px; }
  }
  .chat-messages { flex: 1; overflow-y: auto; padding: 12px; background: #F9FAFB; }
  .chat-msg {
    margin-bottom: 12px;
    &.user { text-align: right; .msg-content { background: #4D6BFE; color: #fff; display: inline-block; padding: 10px 14px; border-radius: 12px 12px 4px 12px; max-width: 85%; text-align: left; font-size: 13px; white-space: pre-wrap; } }
    &.assistant { text-align: left; .msg-content { background: #fff; display: inline-block; padding: 10px 14px; border-radius: 12px 12px 12px 4px; max-width: 85%; box-shadow: 0 1px 3px rgba(0,0,0,0.06); font-size: 13px; white-space: pre-wrap; color: #303133; border: 1px solid #E5E7EB; &.typing { color: #909399; } } }
  }
  .chat-input { display: flex; gap: 8px; padding: 12px; border-top: 1px solid #E5E7EB; }
}
</style>
