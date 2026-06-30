<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-right">
        <el-button @click="loadDocuments" :icon="Refresh">刷新</el-button>
        <el-upload
          :before-upload="handleUpload"
          :show-file-list="false"
          accept=".txt,.md,.pdf,.docx,.pptx,.html,.csv,.json"
          action="#"
        >
          <el-button type="primary">上传文档</el-button>
        </el-upload>
      </div>
    </div>

    <el-table :data="documents" stripe v-loading="loading" empty-text="暂无知识文档">
      <el-table-column prop="file_name" label="文件名" min-width="180" />
      <el-table-column prop="chunks" label="块数" width="80" align="center" />
      <el-table-column label="预览" width="80" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="previewFile(row)">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.upload_time) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showChunks(row.id)">明细</el-button>
          <el-button type="warning" size="small" link @click="handleRevectorize(row)">重新向量化</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 向量明细弹窗 -->
    <el-dialog v-model="chunkDialogVisible" :title="'向量明细 — ' + chunkFileTitle" width="700px" top="5vh">
      <el-table :data="chunks" stripe max-height="500" v-loading="chunkLoading" empty-text="暂无数据">
        <el-table-column prop="chunk_index" label="#" width="60" align="center" />
        <el-table-column prop="content" label="内容预览" show-overflow-tooltip min-width="400" />
      </el-table>
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" :title="'文件预览 — ' + previewFileName" width="800px" top="3vh">
      <div class="preview-content" v-loading="previewLoading">
        <pre>{{ previewContent }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import http from "@/utils/request";

const documents = ref<any[]>([]);
const loading = ref(false);
const chunkDialogVisible = ref(false);
const chunkFileTitle = ref("");
const chunks = ref<any[]>([]);
const chunkLoading = ref(false);
const previewVisible = ref(false);
const previewFileName = ref("");
const previewContent = ref("");
const previewLoading = ref(false);

async function loadDocuments() {
  loading.value = true;
  try {
    const res = await http.get("/admin/knowledge");
    documents.value = res.data || [];
  } catch {
    ElMessage.error("加载失败");
  } finally {
    loading.value = false;
  }
}

async function handleUpload(file: File) {
  const form = new FormData();
  form.append("file", file);
  const ext = file.name.split(".").pop()?.toLowerCase();
  const supported = ["txt", "md", "pdf", "docx", "pptx", "html", "csv", "json"];
  if (!supported.includes(ext || "")) {
    ElMessage.warning(`不支持的文件类型: .${ext}`);
    return false;
  }
  try {
    await http.post("/admin/knowledge/upload", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    ElMessage.success(`上传成功: ${file.name}`);
    loadDocuments();
  } catch {
    ElMessage.error("上传失败");
  }
  return false;
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm("确定删除 \"" + row.file_name + "\"？", "确认");
    await http.delete(`/admin/knowledge/${encodeURIComponent(row.id)}`);
    ElMessage.success("已删除");
    loadDocuments();
  } catch { /* cancelled */ }
}

async function handleRevectorize(row: any) {
  try {
    await ElMessageBox.confirm("确定重新向量化 \"" + row.file_name + "\"？将删除旧块并重新解析。", "确认");
    await http.post(`/admin/knowledge/${encodeURIComponent(row.id)}/revectorize`);
    ElMessage.success("重新向量化成功");
    loadDocuments();
  } catch { /* cancelled */ }
}

async function previewFile(row: any) {
  previewFileName.value = row.file_name;
  previewVisible.value = true;
  previewLoading.value = true;
  try {
    const res = await http.get(`/admin/knowledge/${encodeURIComponent(row.id)}/preview`);
    previewContent.value = res.data?.content || "(空文件)";
  } catch {
    previewContent.value = "加载失败";
  } finally {
    previewLoading.value = false;
  }
}

async function showChunks(fileId: string) {
  chunkFileTitle.value = fileId;
  chunkDialogVisible.value = true;
  chunkLoading.value = true;
  try {
    const res = await http.get(`/admin/knowledge/chunks/${encodeURIComponent(fileId)}`);
    chunks.value = res.data || [];
  } catch {
    ElMessage.error("加载块详情失败");
  } finally {
    chunkLoading.value = false;
  }
}

function formatTime(time: string): string {
  if (!time) return "";
  const d = new Date(time);
  return d.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

onMounted(loadDocuments);
</script>

<style scoped>
.knowledge-page {
  background: rgba(30, 41, 59, 0.95);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}
.page-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 20px; 
}
.page-header h2 { 
  margin: 0; 
  font-size: 18px; 
  font-weight: 600;
  color: #F8FAFC;
  display: flex;
  align-items: center;
  gap: 12px;

  &::before {
    content: "";
    width: 4px;
    height: 20px;
    background: linear-gradient(180deg, #4F46E5, #06B6D4);
    border-radius: 2px;
  }
}
.header-right { display: flex; gap: 12px; align-items: center; }
.preview-content { max-height: 70vh; overflow: auto; }
.preview-content pre { 
  white-space: pre-wrap; 
  word-break: break-all; 
  font-size: 13px; 
  line-height: 1.6; 
  color: #E2E8F0; 
  background: rgba(15, 23, 42, 0.8); 
  padding: 16px; 
  border-radius: 8px; 
}
</style>
