<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-head">
        <el-button type="primary" size="small" @click="newChat" :disabled="streaming" class="new-chat-btn">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>
      <div class="sidebar-list">
        <div v-if="conversations.length === 0" class="sidebar-empty">暂无历史会话</div>
        <div
          v-for="conv in conversations"
          :key="conv.chat_id"
          class="sidebar-item"
          :class="{ active: conv.chat_id === chatId }"
          @click="switchConversation(conv)"
        >
          <div class="si-body">
            <span class="si-title">{{ conv.title || '新对话' }}</span>
            <span class="si-date">{{ formatDate(conv.created_at) }}</span>
          </div>
          <el-button
            size="small"
            text
            :icon="Delete"
            @click.stop="deleteConversation(conv)"
            class="si-del"
          />
        </div>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-topbar">
        <div class="ct-left">
          <span class="ct-title">AI 助手</span>
          <span class="ct-sub">智能英语学习助手</span>
        </div>
        <div class="ct-right">
          <el-button size="small" class="mode-btn" @click="toggleModel">{{ model === 'deepseek' ? 'DeepSeek' : '通义千问' }}</el-button>
          <el-button
            size="small"
            :class="['mode-btn', { active: promptMode === 'agent' }]"
            @click="togglePromptMode"
          >
            {{ promptMode === 'agent' ? 'Agent' : '聊天' }}
          </el-button>
          <el-button size="small" class="mode-btn" @click="toggleChatMode">{{ chatMode === 'stream' ? '流式' : '同步' }}</el-button>
        </div>
      </div>

      <div class="chat-messages" ref="msgContainer">
        <div v-if="messages.length === 0" class="chat-placeholder">
          <div class="cp-icon-wrapper">
            <div class="cp-icon">🤖</div>
          </div>
          <div class="cp-title">有什么英语学习问题？</div>
          <div class="cp-hint">可以问单词释义、语法规则、翻译等</div>
        </div>

        <div
          v-for="(msg, mi) in messages"
          :key="'msg-' + mi"
          class="msg-row"
          :class="{ 'msg-user': msg.role === 'user' }"
        >
          <div class="msg-avatar" v-if="msg.role === 'assistant'">🤖</div>
          <div class="msg-bubble" :class="{ streaming: mi === messages.length - 1 && streaming }">
            <div class="msg-text">{{ msg.content }}</div>
            <span class="msg-cursor" v-if="mi === messages.length - 1 && streaming">|</span>
          </div>
          <div class="msg-avatar" v-if="msg.role === 'user'">😊</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="输入你的问题，Enter 发送"
          size="large"
          :disabled="streaming"
          @keyup.enter.exact="sendMessage"
          clearable
          class="chat-input-field"
        >
          <template #append>
            <el-button
              type="primary"
              :disabled="!inputText.trim() || streaming"
              @click="sendMessage"
              class="send-btn"
            >
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Delete } from "@element-plus/icons-vue";
import http from "@/utils/request";

const messages = ref<{ role: string; content: string }[]>([]);
const inputText = ref("");
const streaming = ref(false);
const chatMode = ref<"stream" | "sync">("stream");
const promptMode = ref<"chat" | "agent">("chat");
const model = ref<"deepseek" | "dashscope">("deepseek");
const chatId = ref(newChatId());
const conversations = ref<any[]>([]);
const msgContainer = ref<HTMLElement | null>(null);

let sseBuffer = "";

onMounted(async () => {
  await loadChatPreferences();
  await loadConversations();
  if (conversations.value.length > 0) {
    switchConversation(conversations.value[0]);
  }
});

watch(messages, () => scrollToBottom(), { deep: true });
watch([model, chatMode, promptMode], () => saveChatPreferences());

function newChatId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 8);
}

async function loadChatPreferences() {
  try {
    const res = await http.get("/checkin/settings/chat");
    if (res.data) {
      model.value = (res.data.chat_model as any) || "deepseek";
      chatMode.value = res.data.chat_stream ? "stream" : "sync";
    }
  } catch { /* ignore */ }
}

async function saveChatPreferences() {
  try {
    await http.put("/checkin/settings/chat", {
      chat_model: model.value,
      chat_stream: chatMode.value === "stream",
    });
  } catch { /* ignore */ }
}

function newChat() {
  if (streaming.value) return;
  messages.value = [];
  chatId.value = newChatId();
  loadConversations();
}

async function loadConversations() {
  try {
    const res = await http.get("/chat/conversations");
    conversations.value = res.data || [];
  } catch { /* ignore */ }
}

function switchConversation(conv: any) {
  if (streaming.value) return;
  chatId.value = conv.chat_id;
  messages.value = [];
  loadHistory(conv.chat_id);
}

async function loadHistory(cid: string) {
  try {
    const res = await http.get(`/chat/messages/${cid}`, {
      params: { mode: promptMode.value }
    });
    messages.value = (res.data || []).map((m: any) => ({
      role: m.role,
      content: m.content,
    }));
    scrollToBottom();
  } catch { /* ignore */ }
}

async function deleteConversation(conv: any) {
  try {
    await ElMessageBox.confirm("确定删除该会话？", "删除会话", { type: "warning" });
  } catch { return; }
  try {
    await http.delete(`/chat/conversations/${conv.chat_id}`);
    conversations.value = conversations.value.filter((c) => c.chat_id !== conv.chat_id);
    if (chatId.value === conv.chat_id) {
      chatId.value = newChatId();
      messages.value = [];
    }
    ElMessage.success("已删除");
  } catch { ElMessage.error("删除失败"); }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
}

async function sendMessage() {
  const text = inputText.value.trim();
  if (!text || streaming.value) return;

  inputText.value = "";
  messages.value.push({ role: "user", content: text });
  messages.value.push({ role: "assistant", content: "" });
  scrollToBottom();

  const lastIdx = messages.value.length - 1;

  if (chatMode.value === "sync") {
    await sendSync(text, lastIdx);
    return;
  }

  await sendStream(text, lastIdx);
}

function togglePromptMode() {
  if (streaming.value) return;
  promptMode.value = promptMode.value === "chat" ? "agent" : "chat";
  loadConversations();
  if (chatId.value) loadHistory(chatId.value);
}

function toggleChatMode() {
  if (streaming.value) return;
  chatMode.value = chatMode.value === "stream" ? "sync" : "stream";
}

function toggleModel() {
  if (streaming.value) return;
  model.value = model.value === "deepseek" ? "dashscope" : "deepseek";
}

async function sendSync(text: string, lastIdx: number) {
  streaming.value = true;
  try {
    const body: any = {
      message: text,
      model: model.value,
      chat_id: chatId.value,
    };
    if (promptMode.value === "agent") body.mode = "agent";
    const res = await http.post("/chat/call", body);
    const reply = typeof res.data === "string" ? res.data : (res.data?.reply ?? res.data ?? "(无响应)");
    updateMessage(lastIdx, String(reply));
  } catch (e: any) {
    updateMessage(lastIdx, "请求失败: " + (e?.message || "网络错误"));
  } finally {
    streaming.value = false;
  }
}

async function sendStream(text: string, lastIdx: number) {
  streaming.value = true;
  sseBuffer = "";

  const token = localStorage.getItem("admin_token");
  const body: any = {
    message: text,
    model: model.value,
    chat_id: chatId.value,
  };
  if (promptMode.value === "agent") body.mode = "agent";

  try {
    const response = await fetch("/api/v1/chat/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const reader = response.body?.getReader();
    if (!reader) throw new Error("不支持流式");

    const decoder = new TextDecoder();
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      sseBuffer += decoder.decode(value, { stream: true });
      const events = sseBuffer.split("\n\n");
      sseBuffer = events.pop() || "";

      for (const part of events) {
        const data = part.replace(/^data:\s*/, "").trim();
        if (data) {
          const current = messages.value[lastIdx].content + data;
          updateMessage(lastIdx, current);
        }
      }
    }
  } catch (e: any) {
    updateMessage(lastIdx, "请求失败: " + (e?.message || "网络错误"));
  } finally {
    streaming.value = false;
  }
}

function updateMessage(index: number, content: string) {
  messages.value[index] = { ...messages.value[index], content };
}

function scrollToBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight;
    }
  });
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 120px);
  background: #1E293B;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.chat-sidebar {
  width: 280px;
  min-width: 280px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(15, 23, 42, 0.8);
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(20px);
}

.sidebar-head {
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.new-chat-btn {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(135deg, #4F46E5, #3730A3);
  border: none;
  font-weight: 600;
  height: 40px;
  
  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #5B53D6, #3730A3);
    box-shadow: 0 4px 15px rgba(79, 70, 229, 0.4);
  }
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.sidebar-empty {
  text-align: center;
  color: #64748B;
  font-size: 13px;
  padding: 60px 0;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(79, 70, 229, 0.1);
  }
  
  &.active {
    background: rgba(79, 70, 229, 0.15);
    border-left: 3px solid #4F46E5;
    
    .si-title { color: #818CF8; }
  }

  .si-body {
    flex: 1;
    overflow: hidden;
    
    .si-title {
      font-size: 14px;
      font-weight: 500;
      color: #CBD5E1;
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .si-date {
      font-size: 12px;
      color: #64748B;
      display: block;
      margin-top: 4px;
    }
  }

  .si-del {
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s;
    color: #94A3B8;
    
    &:hover {
      color: #EF4444;
    }
  }
  
  &:hover .si-del { opacity: 1; }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #0F172A;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  background: rgba(30, 41, 59, 0.5);
  backdrop-filter: blur(10px);

  .ct-left {
    display: flex;
    flex-direction: column;
    gap: 2px;
    
    .ct-title { 
      font-size: 16px; 
      font-weight: 600; 
      color: #F8FAFC; 
    }
    
    .ct-sub { 
      font-size: 12px; 
      color: #64748B; 
    }
  }
  
  .ct-right {
    display: flex;
    gap: 8px;
  }
}

.mode-btn {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94A3B8;
  
  &.active {
    background: rgba(239, 68, 68, 0.15);
    border-color: rgba(239, 68, 68, 0.3);
    color: #F87171;
  }
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: 
    radial-gradient(circle at 20% 80%, rgba(79, 70, 229, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(6, 182, 212, 0.03) 0%, transparent 50%);
}

.chat-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120px;
  
  .cp-icon-wrapper {
    width: 80px;
    height: 80px;
    border-radius: 24px;
    background: linear-gradient(135deg, rgba(79, 70, 229, 0.2), rgba(6, 182, 212, 0.2));
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    box-shadow: 0 8px 32px rgba(79, 70, 229, 0.15);
    animation: iconFloat 3s ease-in-out infinite;
  }
  
  .cp-icon { 
    font-size: 40px; 
  }
  
  .cp-title { 
    font-size: 18px; 
    color: #94A3B8; 
    font-weight: 500; 
  }
  
  .cp-hint { 
    font-size: 13px; 
    color: #64748B; 
    margin-top: 8px; 
  }
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.msg-row {
  display: flex;
  margin-bottom: 24px;
  
  &.msg-user { 
    flex-direction: row-reverse; 
  }
}

.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.msg-user .msg-avatar {
  margin-left: 12px;
  background: rgba(79, 70, 229, 0.2);
  border-color: rgba(79, 70, 229, 0.3);
}

.msg-row:not(.msg-user) .msg-avatar {
  margin-right: 12px;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.2);
}

.msg-bubble {
  max-width: 75%;
  padding: 16px 20px;
  border-radius: 20px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);

  .msg-text {
    font-size: 14px;
    color: #E2E8F0;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &.streaming {
    background: rgba(79, 70, 229, 0.1);
    border-color: rgba(79, 70, 229, 0.2);
    
    .msg-text {
      color: #F8FAFC;
    }
  }
}

.msg-user .msg-bubble {
  background: linear-gradient(135deg, #4F46E5, #3730A3);
  border-color: transparent;
  
  .msg-text { 
    color: #fff; 
  }
}

.msg-cursor {
  display: inline;
  color: #818CF8;
  font-weight: 700;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.chat-input {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  background: rgba(30, 41, 59, 0.5);
}

.chat-input-field {
  .el-input__wrapper {
    border-radius: 16px;
    background: rgba(15, 23, 42, 0.8);
    border-color: rgba(255, 255, 255, 0.08);
    
    &:hover {
      border-color: rgba(79, 70, 229, 0.3);
    }
    
    &.is-focus {
      border-color: #4F46E5;
      box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
    }
  }
  
  .el-input__inner {
    color: #F8FAFC;
    font-size: 14px;
  }
}

.send-btn {
  border-radius: 12px;
  background: linear-gradient(135deg, #4F46E5, #3730A3);
  border: none;
  font-weight: 600;
  
  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #5B53D6, #3730A3);
    box-shadow: 0 4px 15px rgba(79, 70, 229, 0.4);
  }
}
</style>