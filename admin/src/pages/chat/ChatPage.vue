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
          <el-button size="small" type="warning" @click="showDebugPanel = !showDebugPanel">测验调试</el-button>
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

    <!-- 测验题调试面板 -->
    <div class="debug-panel" v-if="showDebugPanel">
      <div class="dp-head">
        <span>测验题生成调试</span>
        <el-button size="small" text @click="showDebugPanel = false">✕</el-button>
      </div>
      <div class="dp-body">
        <div class="dp-row">
          <el-input v-model="debugPrompt" placeholder="粘贴完整 prompt" size="small" type="textarea" :rows="3" style="flex:1" />
          <el-select v-model="debugModel" size="small" style="width:110px">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="DashScope" value="dashscope" />
          </el-select>
          <el-button size="small" type="primary" @click="runDebug" :loading="debugLoading">生成</el-button>
        </div>
        <div v-if="debugResult" class="dp-result">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="耗时">{{ debugResult.durationMs }}ms</el-descriptions-item>
            <el-descriptions-item label="LLM 返回"><pre class="dp-pre">{{ debugResult.response }}</pre></el-descriptions-item>
          </el-descriptions>
        </div>
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

// 测验调试
const showDebugPanel = ref(false);
const debugPrompt = ref("");
const debugModel = ref("deepseek");
const debugLoading = ref(false);
const debugResult = ref<any>(null);

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

async function runDebug() {
  debugLoading.value = true;
  debugResult.value = null;
  try {
    const res = await http.post("/test/debug/generate-question", {
      prompt: debugPrompt.value,
      model: debugModel.value,
    });
    debugResult.value = res.data;
  } catch (e: any) {
    debugResult.value = { error: e?.response?.data?.detail || e?.message || "请求失败" };
  } finally {
    debugLoading.value = false;
  }
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
  background: #FFFFFF;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #E5E7EB;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.chat-sidebar {
  width: 280px;
  min-width: 280px;
  border-right: 1px solid #E5E7EB;
  background: #FAFBFC;
  display: flex;
  flex-direction: column;
}

.sidebar-head {
  padding: 16px;
  border-bottom: 1px solid #E5E7EB;
}

.new-chat-btn {
  width: 100%;
  border-radius: 10px;
  background: #4D6BFE;
  border: none;
  font-weight: 600;
  height: 40px;
  box-shadow: 0 2px 6px rgba(77, 107, 254, 0.2);

  &:hover:not(:disabled) {
    background: #3B5BDB;
    box-shadow: 0 4px 12px rgba(77, 107, 254, 0.3);
  }
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.sidebar-empty {
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
  padding: 60px 0;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #F3F4F6;
  transition: all 0.2s ease;

  &:hover {
    background: #F3F4F6;
  }

  &.active {
    background: #EEF2FF;
    border-left: 3px solid #4D6BFE;

    .si-title { color: #4D6BFE; font-weight: 600; }
  }

  .si-body {
    flex: 1;
    overflow: hidden;

    .si-title {
      font-size: 14px;
      font-weight: 500;
      color: #1F2937;
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .si-date {
      font-size: 12px;
      color: #9CA3AF;
      display: block;
      margin-top: 4px;
    }
  }

  .si-del {
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s;
    color: #9CA3AF;

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
  background: #FFFFFF;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  border-bottom: 1px solid #E5E7EB;
  flex-shrink: 0;
  background: #FFFFFF;

  .ct-left {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .ct-title {
      font-size: 16px;
      font-weight: 600;
      color: #1F2937;
    }

    .ct-sub {
      font-size: 12px;
      color: #9CA3AF;
    }
  }

  .ct-right {
    display: flex;
    gap: 8px;
  }
}

.mode-btn {
  border-radius: 8px;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  color: #6B7280;

  &.active {
    background: rgba(239, 68, 68, 0.08);
    border-color: rgba(239, 68, 68, 0.3);
    color: #DC2626;
  }

  &:hover {
    background: #F3F4F6;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background:
    radial-gradient(circle at 20% 80%, rgba(77, 107, 254, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.03) 0%, transparent 50%);
}

.chat-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120px;

  .cp-icon-wrapper {
    width: 80px;
    height: 80px;
    border-radius: 20px;
    background: linear-gradient(135deg, rgba(77, 107, 254, 0.12), rgba(139, 92, 246, 0.12));
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    box-shadow: 0 4px 16px rgba(77, 107, 254, 0.1);
    animation: iconFloat 3s ease-in-out infinite;
  }

  .cp-icon {
    font-size: 40px;
  }

  .cp-title {
    font-size: 18px;
    color: #6B7280;
    font-weight: 500;
  }

  .cp-hint {
    font-size: 13px;
    color: #9CA3AF;
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
  border-radius: 10px;
  background: #F3F4F6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  border: 1px solid #E5E7EB;
}

.msg-user .msg-avatar {
  margin-left: 12px;
  background: rgba(77, 107, 254, 0.1);
  border-color: rgba(77, 107, 254, 0.2);
}

.msg-row:not(.msg-user) .msg-avatar {
  margin-right: 12px;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.2);
}

.msg-bubble {
  max-width: 75%;
  padding: 14px 18px;
  border-radius: 14px;
  background: #F9FAFB;
  border: 1px solid #E5E7EB;

  .msg-text {
    font-size: 14px;
    color: #1F2937;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &.streaming {
    background: #EEF2FF;
    border-color: rgba(77, 107, 254, 0.2);

    .msg-text {
      color: #1F2937;
    }
  }
}

.msg-user .msg-bubble {
  background: #4D6BFE;
  border-color: #4D6BFE;
  box-shadow: 0 2px 8px rgba(77, 107, 254, 0.2);

  .msg-text {
    color: #fff;
  }
}

.msg-cursor {
  display: inline;
  color: #4D6BFE;
  font-weight: 700;
  animation: blink 0.8s infinite;
}

.debug-panel {
  position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 700px; max-height: 80vh; background: #FFFFFF; border-radius: 12px;
  border: 1px solid #E5E7EB;
  box-shadow: 0 20px 60px rgba(0,0,0,0.1); z-index: 1000;
  display: flex; flex-direction: column;
}
.dp-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid #E5E7EB;
  font-size: 14px; font-weight: 600; color: #1F2937;
}
.dp-body { padding: 16px; overflow-y: auto; }
.dp-row { display: flex; gap: 8px; align-items: center; }
.dp-result { margin-top: 12px; }
.dp-pre {
  margin: 0; padding: 8px; background: #F9FAFB; border-radius: 6px;
  border: 1px solid #E5E7EB;
  font-size: 12px; color: #1F2937; white-space: pre-wrap; word-break: break-all;
  max-height: 200px; overflow-y: auto;
}

@keyframes blink {
  50% { opacity: 0; }
}

.chat-input {
  padding: 16px 24px;
  border-top: 1px solid #E5E7EB;
  flex-shrink: 0;
  background: #FFFFFF;
}

.chat-input-field {
  .el-input__wrapper {
    border-radius: 12px;
    background: #FFFFFF;
    border-color: #E5E7EB;
    box-shadow: 0 0 0 1px #E5E7EB inset;

    &:hover {
      border-color: #D1D5DB;
      box-shadow: 0 0 0 1px #D1D5DB inset;
    }

    &.is-focus {
      border-color: #4D6BFE;
      box-shadow: 0 0 0 1px #4D6BFE inset, 0 0 0 3px rgba(77, 107, 254, 0.12);
    }
  }

  .el-input__inner {
    color: #1F2937;
    font-size: 14px;

    &::placeholder {
      color: #9CA3AF;
    }
  }
}

.send-btn {
  border-radius: 10px;
  background: #4D6BFE;
  border: none;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(77, 107, 254, 0.2);

  &:hover:not(:disabled) {
    background: #3B5BDB;
    box-shadow: 0 4px 12px rgba(77, 107, 254, 0.3);
  }
}
</style>