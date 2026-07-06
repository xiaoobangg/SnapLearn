<template>
  <div class="blog-layout">
    <!-- ====== 左侧：文档树 ====== -->
    <div class="left-panel">
      <div class="panel-head">
        <h4>文档导航</h4>
        <el-switch v-model="onlyMine" size="small" active-text="我的" @change="onOnlyMineChange" />
      </div>
      <div class="tree-wrap">
        <div v-for="node in docTree" :key="node.id"
          :class="['tree-node', { active: selFolder === node.id, 'is-folder': node.docType === 'folder' }]"
          :style="{ paddingLeft: (node.level || 0) * 18 + 12 + 'px' }"
          @click="onNodeClick(node)"
        >
          <span class="node-arrow" v-if="node.docType === 'folder'" @click.stop="toggleNode(node)">
            <el-icon :size="12"><ArrowDown v-if="node.expanded" /><ArrowRight v-else /></el-icon>
          </span>
          <span class="node-arrow" v-else />
          <span class="node-icon">
            <el-icon v-if="node.docType === 'folder'" :size="15"><Folder /></el-icon>
            <el-icon v-else :size="15"><Document /></el-icon>
          </span>
          <span class="node-title">{{ node.title }}</span>
          <span v-if="node.docType === 'folder'" class="node-count">{{ folderCounts[node.id] || 0 }}</span>
        </div>
        <div v-if="docTree.length === 0" class="empty-tree">暂无文档</div>
      </div>
    </div>

    <!-- ====== 中间：文章列表 ====== -->
    <div class="center-panel">
      <div class="blog-head">
        <h1>{{ selFolderTitle || '全部文章' }}</h1>
      </div>
      <div class="blog-list" v-loading="loading">
        <div class="blog-card" v-for="item in items" :key="item.id" @click="$router.push(`/blog/${item.id}`)">
          <h3>{{ item.title }}</h3>
          <div class="bc-meta">
            <span v-if="item.category">{{ item.category }}</span>
            <span>{{ item.updated_at?.substring(0,10) }}</span>
            <span>{{ item.comment_count || 0 }} 评论</span>
          </div>
          <p class="bc-summary">{{ item.summary }}</p>
        </div>
        <el-empty v-if="!loading && items.length===0" description="暂无文章" />
      </div>
      <div class="blog-pager" v-if="total > size">
        <el-pagination background layout="prev,pager,next" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
      </div>
    </div>

    <!-- ====== 右侧：AI 助手 ====== -->
    <div class="chat-panel" :class="{ collapsed: !showChat }">
      <div class="panel-head">
        <h4>AI 助手</h4>
        <el-button size="small" text class="panel-toggle" @click="showChat = !showChat">
          <el-icon :size="16"><DArrowRight v-if="showChat" /><DArrowLeft v-else /></el-icon>
        </el-button>
        <span class="chat-hint">基于博客文章回答</span>
      </div>
      <div class="chat-messages" ref="chatMsgsRef">
        <div v-for="(msg, i) in chatMessages" :key="i" :class="['chat-msg', msg.role]">
          <div class="msg-content">{{ msg.content }}</div>
        </div>
        <div v-if="chatLoading" class="chat-msg assistant"><div class="msg-content typing">...</div></div>
      </div>
      <div class="chat-input">
        <el-input v-model="chatInput" size="small" placeholder="询问博客内容..." @keyup.enter="sendChat" :disabled="chatLoading" />
        <el-button size="small" type="primary" @click="sendChat" :loading="chatLoading">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from "vue";
import { useRouter } from "vue-router";
import { Folder, Document, ArrowDown, ArrowRight, DArrowRight, DArrowLeft } from "@element-plus/icons-vue";
import http from "@/utils/request";

const router = useRouter();

// ===== 文档树 =====
interface DocItem { id: string; title: string; category: string; status: string; visibility?: string; parentId?: string; docType?: string; expanded?: boolean; level?: number; }
const docs = ref<DocItem[]>([]);
const selFolder = ref("");
const selFolderTitle = ref("");
const onlyMine = ref(false);

function getMyUserId() {
  try { return JSON.parse(localStorage.getItem("admin_info") || "{}").id || ""; } catch { return ""; }
}

async function onOnlyMineChange() {
  await loadTree();
  page.value = 1;
  load();
}

async function loadTree() {
  try {
    const userId = onlyMine.value ? getMyUserId() : "";
    const params = userId ? { userId } : {};
    const res = await http.get("/public/tree", { params });
    docs.value = (res.data || []).map((d: any) => normalizeDoc(d));
    for (const d of docs.value) { if (d.docType === 'folder') d.expanded = true; }
  } catch { /* */ }
}

// 标准化 snake_case → camelCase
function normalizeDoc(d: any): DocItem {
  return {
    ...d,
    parentId: d.parent_id ?? d.parentId ?? null,
    docType: d.doc_type ?? d.docType ?? 'document',
    sortOrder: d.sort_order ?? d.sortOrder ?? 0,
  };
}

// 每个文件夹下的文档数
const folderCounts = computed(() => {
  const counts: Record<string, number> = {};
  for (const d of docs.value) {
    if (d.docType !== 'folder' && d.parentId) {
      counts[d.parentId] = (counts[d.parentId] || 0) + 1;
    }
  }
  return counts;
});

// 构建树（过滤空文件夹）
const docTree = computed(() => {
  const map = new Map<string, DocItem[]>();
  for (const d of docs.value) {
    const pid = d.parentId || "root";
    if (!map.has(pid)) map.set(pid, []);
    map.get(pid)!.push({ ...d, expanded: d.docType === 'folder' });
  }
  // 递归检查文件夹是否含有文档
  function hasDoc(folderId: string): boolean {
    return (map.get(folderId) || []).some(c => c.docType !== 'folder' || hasDoc(c.id));
  }
  const result: DocItem[] = [];
  function walk(pid: string, level: number) {
    for (const c of (map.get(pid) || [])) {
      if (c.docType === 'folder' && !hasDoc(c.id)) continue; // 跳过空文件夹
      c.level = level;
      result.push(c);
      if (c.expanded && c.docType === 'folder') walk(c.id, level + 1);
    }
  }
  walk("root", 0);
  return result;
});

function toggleNode(node: DocItem) { node.expanded = !node.expanded; }
function onNodeClick(node: DocItem) {
  if (node.docType === 'folder') {
    toggleNode(node);
    selFolder.value = node.id;
    selFolderTitle.value = node.title;
    page.value = 1;
    load();
  } else {
    router.push(`/blog/${node.id}`);
  }
}

// ===== 文章列表 =====
const items = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);

async function load() {
  loading.value = true;
  try {
    // 收集选中文件夹及其子文件夹的所有文档
    const folderIds = selFolder.value ? collectFolderIds(selFolder.value) : [];
    const params: any = { page: page.value, size: size.value };
    if (folderIds.length > 0) params.parentIds = folderIds.join(',');
    if (onlyMine.value) params.userId = getMyUserId();
    const res = await http.get("/public/documents", { params });
    items.value = res.data.items || [];
    total.value = res.data.total || 0;
  } catch { /* */ }
  loading.value = false;
}

function collectFolderIds(folderId: string): string[] {
  const ids = [folderId];
  for (const d of docs.value) {
    if (d.docType === 'folder' && d.parentId === folderId) {
      ids.push(...collectFolderIds(d.id));
    }
  }
  return ids;
}

// ===== AI 对话 =====
const showChat = ref(true);
const chatInput = ref("");
const chatMessages = ref<{ role: string; content: string }[]>([]);
const chatLoading = ref(false);
const chatMsgsRef = ref<HTMLElement>();
const chatId = ref(Date.now().toString(36) + Math.random().toString(36).slice(2, 8));

function scrollChat() {
  nextTick(() => {
    if (chatMsgsRef.value) chatMsgsRef.value.scrollTop = chatMsgsRef.value.scrollHeight;
  });
}

async function sendChat() {
  const msg = chatInput.value.trim();
  if (!msg || chatLoading.value) return;
  chatInput.value = "";
  chatMessages.value.push({ role: "user", content: msg });
  chatMessages.value.push({ role: "assistant", content: "" });
  scrollChat();
  chatLoading.value = true;
  try {
    const resp = await fetch("/api/v1/public/chat/stream", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: msg, model: "deepseek", chat_id: chatId.value }),
    });
    const reader = resp.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    const lastMsg = chatMessages.value[chatMessages.value.length - 1];
    while (reader) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";
      for (const line of lines) {
        if (line.startsWith("data:")) {
          const data = line.slice(5).trim();
          if (data === "[DONE]") continue;
          lastMsg.content += data;
        }
      }
      scrollChat();
    }
  } catch {
    chatMessages.value[chatMessages.value.length - 1].content = "请求失败，请重试";
  }
  chatLoading.value = false;
}

onMounted(async () => {
  await loadTree();
  load();
});
</script>

<style lang="scss" scoped>
.blog-layout {
  display: flex;
  height: 100%;
  margin: -24px;
}

// ===== Left Panel =====
.left-panel {
  width: 260px;
  min-width: 220px;
  background: #FAFBFC;
  border-right: 1px solid #E5E7EB;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid #E5E7EB;
    h4 { margin: 0; font-size: 15px; font-weight: 700; color: #303133; }
    :deep(.el-switch) { flex-shrink: 0; .el-switch__label { font-size: 11px; } }
  }
  .tree-wrap {
    flex: 1;
    overflow-y: auto;
    padding: 4px 0;
  }
  .empty-tree {
    text-align: center;
    color: #909399;
    font-size: 13px;
    padding: 40px 20px;
  }
}

.tree-node {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  font-size: 14px;
  cursor: pointer;
  color: #303133;
  transition: background 0.15s;
  user-select: none;

  &:hover { background: #EBEDF0; }
  &.active { background: #E8EAED; font-weight: 500; }
  &.is-folder { font-weight: 500; }

  .node-arrow {
    width: 18px; height: 18px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0; color: #909399;
  }
  .node-icon {
    width: 22px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0; color: #909399; margin-right: 2px;
  }
  .node-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .node-count {
    font-size: 11px;
    color: #909399;
    background: #E5E7EB;
    border-radius: 10px;
    padding: 0 6px;
    margin-left: 6px;
    flex-shrink: 0;
  }
}

// ===== Center Panel =====
.center-panel {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background: #F7F8FA;
}

.blog-head {
  margin-bottom: 24px;
  h1 { margin: 0; font-size: 22px; }
}

.blog-list { min-height: 200px; }

.blog-card {
  padding: 20px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); }
  h3 { margin: 0 0 8px; font-size: 18px; }
}

.bc-meta { display: flex; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 8px; }
.bc-summary { font-size: 14px; color: #606266; line-height: 1.6; margin: 0; }
.blog-pager { margin-top: 24px; display: flex; justify-content: center; }

// ===== Chat Panel =====
.chat-panel {
  width: 320px;
  min-width: 280px;
  background: #fff;
  border-left: 1px solid #E5E7EB;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.25s ease, min-width 0.25s ease;

  &.collapsed {
    width: 36px;
    min-width: 36px;
    .chat-messages, .chat-input, .chat-hint { display: none; }
    .panel-head { padding: 12px 8px; h4 { display: none; } }
  }

  .panel-head {
    position: relative;
    padding: 14px 16px;
    border-bottom: 1px solid #E5E7EB;
    h4 { margin: 0; font-size: 15px; font-weight: 600; color: #303133; }
    .chat-hint { font-size: 11px; color: #909399; display: block; margin-top: 2px; }
    .panel-toggle { position: absolute; right: 4px; top: 8px; }
  }
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    background: #F9FAFB;
  }
  .chat-msg {
    margin-bottom: 12px;
    &.user {
      text-align: right;
      .msg-content {
        background: #4D6BFE;
        color: #fff;
        display: inline-block;
        padding: 10px 14px;
        border-radius: 12px 12px 4px 12px;
        max-width: 85%;
        text-align: left;
        font-size: 13px;
        white-space: pre-wrap;
      }
    }
    &.assistant {
      text-align: left;
      .msg-content {
        background: #fff;
        display: inline-block;
        padding: 10px 14px;
        border-radius: 12px 12px 12px 4px;
        max-width: 85%;
        box-shadow: 0 1px 3px rgba(0,0,0,0.06);
        font-size: 13px;
        white-space: pre-wrap;
        color: #303133;
        border: 1px solid #E5E7EB;
        &.typing { color: #909399; }
      }
    }
  }
  .chat-input {
    display: flex;
    gap: 8px;
    padding: 12px;
    border-top: 1px solid #E5E7EB;
  }
}
</style>
