<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>文档管理</h3>
      <div>
        <el-button size="small" type="warning" @click="openAi">AI 助手</el-button>
        <el-button size="small" @click="openImport">批量导入 MD</el-button>
        <el-button type="primary" size="small" @click="openCreate">新建文档</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索标题..." size="small" style="width:200px" clearable @clear="load" @keyup.enter="load" />
      <el-select v-model="filterCategory" placeholder="分类" size="small" style="width:140px" clearable @change="load">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" size="small" style="width:120px" clearable @change="load">
        <el-option label="草稿" value="draft" />
        <el-option label="已发布" value="published" />
        <el-option label="已归档" value="archived" />
      </el-select>
      <el-button size="small" @click="load">搜索</el-button>
    </div>

    <el-table :data="items" stripe v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="category" label="分类" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.category">{{ row.category }}</span>
          <span v-else style="color:#c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status==='draft'" size="small" type="info">草稿</el-tag>
          <el-tag v-else-if="row.status==='published'" size="small" type="success">已发布</el-tag>
          <el-tag v-else-if="row.status==='archived'" size="small" type="warning">已归档</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="170" align="center">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status==='draft'" size="small" link type="success" @click="doPublish(row)">发布</el-button>
          <el-button v-if="row.status==='published'" size="small" link type="warning" @click="doUnpublish(row)">撤销</el-button>
          <el-button size="small" link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pager-wrap" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @change="load" />
    </div>

    <div class="batch-bar" v-if="selected.length > 0">
      <el-button type="success" size="small" @click="doBatchPublish">批量发布 ({{ selected.length }})</el-button>
    </div>

    <!-- 批量导入 Dialog -->
    <el-dialog v-model="importVisible" title="批量导入 MD 文件" width="450px">
      <el-upload ref="uploadRef" drag multiple :auto-upload="false" accept=".md,.markdown,.txt" :on-change="onFileChange">
        <el-icon><UploadFilled /></el-icon>
        <div>拖拽或点击选择 .md 文件</div>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="doImport" :loading="importing" :disabled="importFiles.length===0">导入 ({{ importFiles.length }})</el-button>
      </template>
    </el-dialog>

    <!-- AI 助手 Dialog -->
    <el-dialog v-model="aiVisible" title="AI 文档助手" width="700px" class="ai-dialog">
      <div class="ai-chat-wrap">
        <div class="ai-messages" ref="aiMessages">
          <div v-for="(msg, i) in aiMessages" :key="i" :class="['ai-msg', msg.role]">
            <div class="ai-msg-content" v-html="msg.content"></div>
            <div v-if="msg.toolResult" class="ai-tool-result">{{ msg.toolResult }}</div>
          </div>
        </div>
        <div class="ai-input-row">
          <el-input v-model="aiInput" placeholder="输入问题，如：搜索关于数据库的文档、帮我写一篇..." @keyup.enter="sendAi" :disabled="aiStreaming" />
          <el-button type="primary" @click="sendAi" :loading="aiStreaming">发送</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { UploadFilled } from "@element-plus/icons-vue";
import { documentApi } from "@/api";

const router = useRouter();
const loading = ref(false);
const items = ref<any[]>([]);
const categories = ref<string[]>([]);
const selected = ref<any[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const searchKeyword = ref("");
const filterCategory = ref("");
const filterStatus = ref("");

// Import
const importVisible = ref(false);
const importFiles = ref<any[]>([]);
const importing = ref(false);
const uploadRef = ref<any>();

// AI
const aiVisible = ref(false);
const aiInput = ref("");
const aiMessages = ref<any[]>([]);
const aiStreaming = ref(false);
const aiMessagesEl = ref<HTMLElement>();

function formatTime(t: string) {
  if (!t) return "";
  return new Date(t).toLocaleString("zh-CN");
}

async function load() {
  loading.value = true;
  try {
    const res = await documentApi.list({
      keyword: searchKeyword.value || undefined,
      category: filterCategory.value || undefined,
      status: filterStatus.value || undefined,
      page: page.value,
      size: size.value,
    });
    items.value = res.data?.items || [];
    total.value = res.data?.total || 0;
  } catch { /* ignore */ }
  loading.value = false;
}

async function loadCategories() {
  try {
    const res = await documentApi.categories();
    categories.value = res.data?.categories || [];
  } catch { /* ignore */ }
}

function onSelect(rows: any[]) { selected.value = rows; }

function openCreate() {
  router.push("/documents/new/edit");
}

function openEdit(row: any) {
  router.push(`/documents/${row.id}/edit`);
}

function openImport() {
  importFiles.value = [];
  importVisible.value = true;
}

function onFileChange(_file: any, fileList: any[]) {
  importFiles.value = fileList.map(f => f.raw).filter(Boolean);
}

async function doImport() {
  if (importFiles.value.length === 0) return;
  importing.value = true;
  try {
    const fd = new FormData();
    importFiles.value.forEach((f: File) => fd.append("files", f));
    await documentApi.importFiles(fd);
    ElMessage.success(`已导入 ${importFiles.value.length} 个文件`);
    importVisible.value = false;
    load();
  } catch { ElMessage.error("导入失败"); }
  importing.value = false;
}

async function doPublish(row: any) {
  try {
    await documentApi.publish(row.id);
    ElMessage.success("已发布到知识库");
    load();
  } catch { ElMessage.error("发布失败"); }
}

async function doUnpublish(row: any) {
  try {
    await documentApi.unpublish(row.id);
    ElMessage.success("已撤销发布");
    load();
  } catch { ElMessage.error("撤销失败"); }
}

async function doDelete(row: any) {
  try { await ElMessageBox.confirm("确定删除？已发布的文档会自动撤销。", "提示", { type: "warning" }); } catch { return; }
  try {
    await documentApi.delete(row.id);
    ElMessage.success("已删除");
    load();
  } catch { ElMessage.error("删除失败"); }
}

async function doBatchPublish() {
  try {
    const ids = selected.value.map(r => r.id);
    await documentApi.batchPublish(ids);
    ElMessage.success(`已发布 ${ids.length} 篇文档`);
    load();
  } catch { ElMessage.error("批量发布失败"); }
}

// ===== AI Chat =====
let aiChatId = "";

function openAi() {
  aiVisible.value = true;
  aiMessages.value = [{ role: "assistant", content: "你好！我是文档助手。你可以让我搜索文档、创建新文档、或者修改已有的文档。" }];
  nextTick(() => scrollAi());
}

function scrollAi() {
  nextTick(() => {
    if (aiMessagesEl.value) aiMessagesEl.value.scrollTop = aiMessagesEl.value.scrollHeight;
  });
}

async function sendAi() {
  const msg = aiInput.value.trim();
  if (!msg || aiStreaming.value) return;
  aiInput.value = "";
  aiMessages.value.push({ role: "user", content: msg });
  aiMessages.value.push({ role: "assistant", content: "", toolResult: "" });
  scrollAi();
  aiStreaming.value = true;

  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/admin/documents/ai/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
      body: JSON.stringify({ message: msg, chat_id: aiChatId || undefined }),
    });

    const reader = resp.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    const lastMsg = aiMessages.value[aiMessages.value.length - 1];

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
          try {
            const parsed = JSON.parse(data);
            if (parsed.type === "TOOL_RESULT") {
              lastMsg.toolResult = parsed.content;
            } else {
              lastMsg.content += data;
            }
          } catch {
            lastMsg.content += data;
          }
        } else if (line.startsWith("event:done")) {
          // done event
        } else if (line.startsWith("data:") && line.includes("chat_id")) {
          try {
            const d = JSON.parse(line.slice(5).trim());
            if (d.chat_id) aiChatId = d.chat_id;
          } catch { /* ignore */ }
        }
      }
      scrollAi();
    }
  } catch (e) {
    aiMessages.value[aiMessages.value.length - 1].content = "请求失败，请重试";
  }
  aiStreaming.value = false;
}

onMounted(() => {
  load();
  loadCategories();
});
</script>

<style lang="scss" scoped>

.page-wrap { 
  background: $card-bg;
  border-radius: $radius-lg;
  padding: 24px;
  border: 1px solid $card-border;
  box-shadow: $card-shadow;

  &:hover {
    box-shadow: $card-shadow-hover;
  }
}

.page-head { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 20px;

  h3 { 
    margin: 0; 
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    display: flex;
    align-items: center;
    gap: 12px;

    &::before {
      content: "";
      width: 4px;
      height: 20px;
      background: linear-gradient(180deg, $primary-color, $accent-purple);
      border-radius: 2px;
    }
  } 
}

.filter-bar { 
  display: flex; 
  gap: 12px; 
  margin-bottom: 20px;
  padding: 16px;
  background: #F9FAFB;
  border-radius: $radius-md;
  border: 1px solid $card-border;
}

.pager-wrap { 
  margin-top: 20px; 
  display: flex; 
  justify-content: flex-end;
}

.batch-bar { 
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(16, 185, 129, 0.06);
  border-radius: $radius-md;
  border: 1px solid rgba(16, 185, 129, 0.2);
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-dialog {
  .ai-chat-wrap { 
    display: flex; 
    flex-direction: column; 
    height: 450px;
  }

  .ai-messages { 
    flex: 1; 
    overflow-y: auto; 
    padding: 12px;
    background: #F9FAFB; 
    border-radius: $radius-md; 
    margin-bottom: 16px;
    border: 1px solid $card-border;
  }

  .ai-msg { 
    margin-bottom: 12px;

    &.user { 
      text-align: right;

      .ai-msg-content { 
        background: $primary-color; 
        color: #FFFFFF; 
        display: inline-block; 
        padding: 10px 14px; 
        border-radius: $radius-lg;
        border-bottom-right-radius: 4px;
        max-width: 80%; 
        text-align: left;
        font-size: 13px;
        box-shadow: 0 2px 6px rgba(77, 107, 254, 0.2);
      } 
    }

    &.assistant { 
      text-align: left;

      .ai-msg-content { 
        background: $card-bg; 
        display: inline-block; 
        padding: 10px 14px; 
        border-radius: $radius-lg;
        border-bottom-left-radius: 4px;
        max-width: 80%; 
        box-shadow: 0 1px 3px rgba(0,0,0,0.04); 
        white-space: pre-wrap;
        font-size: 13px;
        color: $text-primary;
        border: 1px solid $card-border;
      } 
    } 
  }

  .ai-tool-result { 
    font-size: 12px; 
    color: $accent-green; 
    margin-top: 4px; 
    padding: 4px 10px; 
    background: rgba(16, 185, 129, 0.1);
    border-radius: $radius-sm; 
    display: inline-block;
  }

  .ai-input-row { 
    display: flex; 
    gap: 10px;
  }
}
</style>
