<template>
  <div class="doc-layout">
    <!-- ====== 左侧：文档树 ====== -->
    <div class="left-panel">
      <div class="panel-head">
        <h4>文档列表</h4>
        <div class="head-actions">
          <el-button size="small" text @click="openCreate"><el-icon><Plus /></el-icon></el-button>
          <el-button size="small" text @click="openImport"><el-icon><Upload /></el-icon></el-button>
        </div>
      </div>
      <div class="tree-wrap">
        <div v-for="group in docTree" :key="group.category" class="tree-group">
          <div class="tree-group-title" @click="toggleGroup(group.category)">
            <el-icon><component :is="group.expanded ? 'FolderOpened' : 'Folder'" /></el-icon>
            <span>{{ group.category || '未分类' }}</span>
            <span class="group-count">{{ group.docs.length }}</span>
          </div>
          <div v-show="group.expanded" class="tree-items">
            <div
              v-for="doc in group.docs" :key="doc.id"
              :class="['tree-item', { active: currentDoc?.id === doc.id }]"
              @click="selectDoc(doc)">
              <span class="item-title">{{ doc.title }}</span>
              <span class="item-status">
                <el-tag v-if="doc.status==='published'" size="small" type="success" effect="dark">已发布</el-tag>
                <el-tag v-else size="small" type="info">草稿</el-tag>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 中间：文档编辑器 ====== -->
    <div class="center-panel">
      <template v-if="currentDoc">
        <div class="editor-toolbar">
          <el-input v-model="currentDoc.title" size="small" placeholder="标题" style="width:240px" />
          <el-input v-model="currentDoc.category" size="small" placeholder="分类" style="width:120px" clearable />
          <el-input v-model="currentDoc.tags" size="small" placeholder="标签(逗号分隔)" style="width:160px" clearable />
          <div class="toolbar-spacer" />
          <el-tag v-if="currentDoc.status==='published'" size="small" type="success" effect="plain">已发布</el-tag>
          <el-tag v-else size="small" type="info" effect="plain">草稿</el-tag>
          <el-button size="small" type="primary" @click="doSave" :loading="saving">保存并发布</el-button>
          <el-button size="small" type="danger" text @click="doDelete">删除</el-button>
        </div>
        <div class="editor-wrap">
          <div class="editor-pane">
            <div class="pane-label">编辑</div>
            <el-input v-model="currentDoc.content" type="textarea" :rows="0" class="editor-area" placeholder="在此编写 Markdown..." />
          </div>
          <div class="preview-pane">
            <div class="pane-label">预览</div>
            <div class="preview-content" v-html="renderedHtml" />
          </div>
        </div>
      </template>
      <div v-else class="empty-center">
        <el-empty description="选择或新建文档" />
      </div>
    </div>

    <!-- ====== 右侧：AI 助手 ====== -->
    <div class="right-panel">
      <div class="panel-head">
        <h4>AI 助手</h4>
        <div class="ai-toolbar">
          <el-select v-model="currentConvId" size="small" placeholder="会话" style="width:140px" @change="switchConv" clearable>
            <el-option v-for="c in aiConvs" :key="c.chat_id" :label="c.title" :value="c.chat_id" />
          </el-select>
          <el-button size="small" text @click="newAiConv"><el-icon><Plus /></el-icon></el-button>
          <el-button v-if="currentConvId" size="small" text type="danger" @click="deleteAiConv"><el-icon><Delete /></el-icon></el-button>
          <el-button size="small" :type="aiModel==='deepseek'?'primary':'default'" plain @click="toggleAiModel">
            {{ aiModel === 'deepseek' ? 'DS' : 'QW' }}
          </el-button>
        </div>
      </div>
      <div class="ai-messages" ref="aiMsgs">
        <div v-for="(msg, i) in aiMessages" :key="i" :class="['ai-msg', msg.role]">
          <div class="msg-content" v-html="msg.content" />
          <div v-if="msg.tool" class="msg-tool">{{ msg.tool }}</div>
        </div>
        <div v-if="aiLoading" class="ai-msg assistant"><div class="msg-content typing">...</div></div>
      </div>
      <div class="ai-input">
        <el-input v-model="aiInput" size="small" placeholder="搜索/创建/修改文档..." @keyup.enter="sendAi" :disabled="aiLoading" />
        <el-button size="small" type="primary" @click="sendAi" :loading="aiLoading">发送</el-button>
      </div>
    </div>

    <!-- 批量导入 Dialog -->
    <el-dialog v-model="importVisible" title="批量导入 MD" width="420px">
      <el-upload ref="uploadRef" drag multiple :auto-upload="false" accept=".md,.markdown,.txt" :on-change="onFileChange">
        <el-icon><UploadFilled /></el-icon>
        <div>拖拽或点击选择 .md 文件</div>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="doImport" :loading="importing" :disabled="importFiles.length===0">导入 ({{ importFiles.length }})</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Upload, UploadFilled, Delete } from "@element-plus/icons-vue";
import { documentApi } from "@/api";

// ===== 文档树 =====
interface DocItem { id: string; title: string; category: string; status: string; }
const docs = ref<DocItem[]>([]);
const docTree = computed(() => {
  const map = new Map<string, { category: string; expanded: boolean; docs: DocItem[] }>();
  for (const d of docs.value) {
    const cat = d.category || "";
    if (!map.has(cat)) map.set(cat, { category: cat, expanded: true, docs: [] });
    map.get(cat)!.docs.push(d);
  }
  return Array.from(map.values());
});
const expandedGroups = reactive<Set<string>>(new Set());

function toggleGroup(cat: string) {
  const g = docTree.value.find(x => x.category === cat);
  if (g) g.expanded = !g.expanded;
}

// ===== 当前文档 =====
const currentDoc = ref<any>(null);
const saving = ref(false);

async function loadDocs() {
  try {
    const res = await documentApi.list({ size: 500 });
    docs.value = res.data?.items || [];
  } catch { /* ignore */ }
}

async function selectDoc(doc: DocItem) {
  try {
    const res = await documentApi.get(doc.id);
    currentDoc.value = res.data;
  } catch { ElMessage.error("加载失败"); }
}

async function openCreate() {
  currentDoc.value = { id: null, title: "新文档", content: "", category: "", tags: "", status: "draft" };
}

async function doSave() {
  saving.value = true;
  try {
    if (!currentDoc.value.id) {
      const res = await documentApi.create(currentDoc.value);
      currentDoc.value.id = res.data.id;
      currentDoc.value.status = res.data.status;
    } else {
      const res = await documentApi.update(currentDoc.value.id, currentDoc.value);
      currentDoc.value.status = res.data.status;
    }
    ElMessage.success("已保存并发布");
    loadDocs();
  } catch (e: any) { ElMessage.error(e?.response?.data?.detail || "保存失败"); }
  saving.value = false;
}

async function doDelete() {
  try { await ElMessageBox.confirm("确定删除？", "提示", { type: "warning" }); } catch { return; }
  try {
    await documentApi.delete(currentDoc.value.id);
    currentDoc.value = null;
    ElMessage.success("已删除");
    loadDocs();
  } catch { ElMessage.error("删除失败"); }
}

// ===== 批量导入 =====
const importVisible = ref(false);
const importFiles = ref<any[]>([]);
const importing = ref(false);
function openImport() { importFiles.value = []; importVisible.value = true; }
function onFileChange(_f: any, list: any[]) { importFiles.value = list.map(x => x.raw).filter(Boolean); }
async function doImport() {
  if (!importFiles.value.length) return;
  importing.value = true;
  try {
    const fd = new FormData();
    importFiles.value.forEach((f: File) => fd.append("files", f));
    await documentApi.importFiles(fd);
    ElMessage.success(`已导入 ${importFiles.value.length} 个文件`);
    importVisible.value = false;
    loadDocs();
  } catch { ElMessage.error("导入失败"); }
  importing.value = false;
}

// ===== Markdown 预览 =====
const renderedHtml = computed(() => {
  let md = currentDoc.value?.content || "";
  md = md.replace(/^### (.+)$/gm, "<h4>$1</h4>");
  md = md.replace(/^## (.+)$/gm, "<h3>$1</h3>");
  md = md.replace(/^# (.+)$/gm, "<h2>$1</h2>");
  md = md.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  md = md.replace(/\*(.+?)\*/g, "<em>$1</em>");
  md = md.replace(/`([^`]+)`/g, "<code>$1</code>");
  md = md.replace(/\[([^\]]+)\]\(([^)]+)\)/g, "<a href=\"$2\" target=\"_blank\">$1</a>");
  md = md.replace(/```(\w*)\n([\s\S]*?)```/g, "<pre><code>$2</code></pre>");
  md = md.replace(/\n\n/g, "<br><br>");
  md = md.replace(/\n/g, "<br>");
  return md;
});

// ===== AI =====
const aiInput = ref("");
const aiMessages = ref<{ role: string; content: string; tool?: string }[]>([]);
const aiLoading = ref(false);
const aiMsgs = ref<HTMLElement>();
const aiConvs = ref<any[]>([]);
const currentConvId = ref("");
const aiModel = ref<"deepseek" | "dashscope">("deepseek");

async function loadAiConvs() {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/chat/conversations`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    aiConvs.value = await resp.json();
  } catch { /* ignore */ }
}

async function loadAiHistory(chatId: string) {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/chat/messages/${chatId}?mode=chat`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const msgs = await resp.json();
    aiMessages.value = msgs.map((m: any) => ({
      role: m.role === "user" ? "user" : "assistant",
      content: m.content,
    }));
  } catch { aiMessages.value = []; }
}

async function switchConv(chatId: string) {
  if (!chatId) { newAiConv(); return; }
  currentConvId.value = chatId;
  loadAiHistory(chatId);
}

async function newAiConv() {
  currentConvId.value = "";
  aiMessages.value = [{ role: "assistant", content: "你好！我是文档助手。" }];
  loadAiConvs();
}

async function deleteAiConv() {
  if (!currentConvId.value) return;
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    await fetch(`${baseUrl}/api/v1/chat/conversations/${currentConvId.value}`, {
      method: "DELETE", headers: { Authorization: `Bearer ${token}` },
    });
    newAiConv();
  } catch { /* ignore */ }
}

function toggleAiModel() {
  aiModel.value = aiModel.value === "deepseek" ? "dashscope" : "deepseek";
}

function scrollAi() { nextTick(() => { if (aiMsgs.value) aiMsgs.value.scrollTop = aiMsgs.value.scrollHeight; }); }

async function sendAi() {
  const msg = aiInput.value.trim();
  if (!msg || aiLoading.value) return;
  aiInput.value = "";

  // Auto-create conversation on first message
  if (!currentConvId.value) {
    currentConvId.value = Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
  }

  aiMessages.value.push({ role: "user", content: msg });
  aiMessages.value.push({ role: "assistant", content: "" });
  scrollAi();
  aiLoading.value = true;

  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/chat/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
      body: JSON.stringify({ message: msg, chat_id: currentConvId.value, model: aiModel.value, tool_mode: "document" }),
    });
    const reader = resp.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    const last = aiMessages.value[aiMessages.value.length - 1];
    while (reader) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";
      for (const line of lines) {
        if (line.startsWith("data:")) {
          const data = line.slice(5).trim();
          if (data.startsWith("[DONE]")) continue;
          if (data.startsWith("{")) {
            try { const p = JSON.parse(data); if (p.chat_id) currentConvId.value = p.chat_id; } catch { last.content += data; }
          } else { last.content += data; }
        }
      }
      scrollAi();
    }
    loadAiConvs();
  } catch { aiMessages.value[aiMessages.value.length - 1].content = "请求失败"; }
  aiLoading.value = false;
}

onMounted(() => {
  loadDocs();
  loadAiConvs();
  aiMessages.value = [{ role: "assistant", content: "你好！我是文档助手。可以帮你搜索、创建或修改文档。" }];
});
</script>

<style lang="scss" scoped>

.doc-layout { 
  display: flex; 
  height: 100%;
  background: $bg-gradient-start;
}

// ===== Left Panel =====
.left-panel { 
  width: 260px; 
  min-width: 220px; 
  background: $card-bg; 
  border-right: 1px solid $card-border; 
  display: flex; 
  flex-direction: column;
  border-radius: $radius-lg 0 0 $radius-lg;
  overflow: hidden;

  .panel-head { 
    display: flex; 
    align-items: center; 
    justify-content: space-between; 
    padding: 14px 16px; 
    border-bottom: 1px solid $card-border;

    h4 { 
      margin: 0; 
      font-size: 15px; 
      font-weight: 600;
      color: $text-primary;
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: "";
        width: 4px;
        height: 16px;
        background: linear-gradient(180deg, $primary-color, $accent-purple);
        border-radius: 2px;
      }
    }

    .head-actions { 
      display: flex; 
      gap: 4px; 
    }
  }

  .tree-wrap { 
    flex: 1; 
    overflow-y: auto; 
    padding: 8px 0;
  }

  .tree-group { margin-bottom: 4px; }

  .tree-group-title { 
    display: flex; 
    align-items: center; 
    gap: 8px; 
    padding: 8px 16px; 
    font-size: 13px; 
    color: $text-secondary; 
    cursor: pointer; 
    user-select: none;
    border-radius: $radius-sm;
    margin: 0 8px;

    &:hover { 
      background: $sidebar-hover;
      color: $text-primary;
    }

    .el-icon { 
      font-size: 15px; 
      color: $text-muted;
    }

    .group-count { 
      margin-left: auto; 
      font-size: 12px; 
      color: $text-muted; 
      background: #F3F4F6;
      padding: 2px 6px;
      border-radius: 10px;
    } 
  }

  .tree-items { padding-left: 8px; }

  .tree-item { 
    display: flex; 
    align-items: center; 
    padding: 8px 16px 8px 32px; 
    font-size: 13px; 
    cursor: pointer; 
    border-radius: $radius-sm; 
    margin: 2px 8px;
    color: $text-primary;

    &:hover { 
      background: $sidebar-hover;
    }

    &.active { 
      background: $sidebar-active; 
      color: $primary-color;
      font-weight: 500;
    }

    .item-title { 
      flex: 1; 
      overflow: hidden; 
      text-overflow: ellipsis; 
      white-space: nowrap; 
    }

    .item-status { margin-left: 8px; } 
  }
}

// ===== Center Panel =====
.center-panel { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  overflow: hidden;
  background: $card-bg;
  border-left: 1px solid $card-border;
}

.editor-toolbar { 
  display: flex; 
  align-items: center; 
  gap: 10px; 
  padding: 12px 16px; 
  background: $card-bg; 
  border-bottom: 1px solid $card-border; 
  flex-wrap: wrap;

  .toolbar-spacer { flex: 1; } 
}

.editor-wrap { 
  flex: 1; 
  display: flex; 
  overflow: hidden;

  .editor-pane, .preview-pane { 
    flex: 1; 
    display: flex; 
    flex-direction: column;
  }

  .editor-pane { border-right: 1px solid $card-border; }

  .pane-label { 
    padding: 8px 14px; 
    background: #F9FAFB; 
    font-size: 13px; 
    color: $text-secondary;
    font-weight: 500;
    border-bottom: 1px solid $card-border; 
  }

  .editor-area {
    flex: 1;
    background: #FFFFFF;

    :deep(textarea) { 
      border: none; 
      border-radius: 0; 
      resize: none; 
      font-family: 'Menlo', 'Consolas', monospace; 
      font-size: 13px; 
      line-height: 1.7; 
      height: 100% !important;
      background: #FFFFFF;
    }
  }

  .preview-content { 
    flex: 1; 
    overflow-y: auto; 
    padding: 16px; 
    background: #FFFFFF; 
    font-size: 14px; 
    line-height: 1.8;

    :deep(h2) { font-size: 20px; margin: 16px 0 10px; color: $text-primary; font-weight: 600; }
    :deep(h3) { font-size: 17px; margin: 14px 0 8px; color: $text-primary; font-weight: 600; }
    :deep(h4) { font-size: 15px; margin: 10px 0 6px; color: $text-primary; font-weight: 500; }
    :deep(code) { background: #F3F4F6; padding: 2px 6px; border-radius: 4px; font-size: 13px; color: $accent-red; }
    :deep(pre) { 
      background: #1F2937; 
      color: #E5E7EB; 
      padding: 16px; 
      border-radius: $radius-md; 
      overflow-x: auto;
      font-family: 'Menlo', 'Consolas', monospace;
      font-size: 13px;

      code { background: none; padding: 0; color: inherit; } 
    }
    :deep(a) { color: $primary-color; text-decoration: none;
      &:hover { text-decoration: underline; }
    }
    :deep(p) { margin: 8px 0; }
  }
}

.empty-center { 
  flex: 1; 
  display: flex; 
  align-items: center; 
  justify-content: center;
  background: $bg-gradient-start;
}

// ===== Right Panel =====
.right-panel {
  width: 340px;
  min-width: 300px;
  background: $card-bg;
  border-left: 1px solid $card-border;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-head { 
    padding: 14px 16px; 
    border-bottom: 1px solid $card-border;

    h4 { 
      margin: 0; 
      font-size: 15px; 
      font-weight: 600;
      color: $text-primary;
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: "";
        width: 4px;
        height: 16px;
        background: linear-gradient(180deg, $primary-color, $accent-purple);
        border-radius: 2px;
      }
    }
    .ai-toolbar { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; margin-top: 8px; }
  }

  .ai-messages { 
    flex: 1; 
    overflow-y: auto; 
    padding: 12px;
    background: #F9FAFB;
  }

  .ai-msg { 
    margin-bottom: 12px;

    &.user { 
      text-align: right;

      .msg-content { 
        background: $primary-color; 
        color: #FFFFFF; 
        display: inline-block; 
        padding: 10px 14px; 
        border-radius: $radius-lg;
        border-bottom-right-radius: 4px;
        max-width: 85%; 
        text-align: left; 
        font-size: 13px; 
        white-space: pre-wrap;
        box-shadow: 0 2px 6px rgba(77, 107, 254, 0.2);
      } 
    }

    &.assistant {
      text-align: left;

      .msg-content { 
        background: $card-bg; 
        display: inline-block; 
        padding: 10px 14px; 
        border-radius: $radius-lg;
        border-bottom-left-radius: 4px;
        max-width: 85%; 
        box-shadow: 0 1px 3px rgba(0,0,0,0.04); 
        font-size: 13px; 
        white-space: pre-wrap;
        color: $text-primary;
        border: 1px solid $card-border;

        &.typing { 
          color: $text-muted; 
        } 
      } 
    } 
  }

  .msg-tool { 
    font-size: 12px; 
    color: $accent-green; 
    margin-top: 4px; 
    padding: 4px 10px; 
    background: rgba(16, 185, 129, 0.1);
    border-radius: $radius-sm; 
    display: inline-block;
  }

  .ai-input { 
    display: flex; 
    gap: 8px; 
    padding: 12px; 
    border-top: 1px solid $card-border;
    background: $card-bg;
  }
}
</style>
