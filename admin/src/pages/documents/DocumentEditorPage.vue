<template>
  <div class="page-wrap">
    <div class="page-head">
      <div class="head-left">
        <el-button size="small" text @click="goBack">← 返回</el-button>
        <h3 v-if="!isNew">编辑文档</h3>
        <h3 v-else>新建文档</h3>
      </div>
      <div>
        <el-button size="small" @click="doSave">保存</el-button>
        <el-button v-if="doc.status==='draft'" type="success" size="small" @click="doPublish">保存并发布</el-button>
        <el-button v-if="doc.status==='published'" type="warning" size="small" @click="doUnpublish">撤销发布</el-button>
      </div>
    </div>

    <el-form label-width="60px">
      <el-form-item label="标题">
        <el-input v-model="doc.title" placeholder="文档标题" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="doc.category" placeholder="选择分类" style="width:200px" clearable allow-create filterable>
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="doc.tags" placeholder="标签，逗号分隔" />
      </el-form-item>
    </el-form>

    <!-- Markdown 编辑器 -->
    <div class="editor-wrap">
      <div class="editor-pane">
        <div class="pane-header">编辑</div>
        <el-input v-model="doc.content" type="textarea" :rows="22" placeholder="在此编写 Markdown 内容..." class="editor-textarea" />
      </div>
      <div class="preview-pane">
        <div class="pane-header">预览</div>
        <div class="preview-content" v-html="renderedContent"></div>
      </div>
    </div>

    <!-- 状态提示 -->
    <div class="status-bar">
      <el-tag v-if="doc.status==='draft'" type="info">草稿</el-tag>
      <el-tag v-else-if="doc.status==='published'" type="success">已发布</el-tag>
      <span v-if="doc.status==='published'" style="margin-left:8px;color:#909399;font-size:13px">修改内容后需要重新发布才能更新知识库</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { documentApi } from "@/api";

const router = useRouter();
const route = useRoute();
const isNew = computed(() => !route.params.id || route.params.id === "new");
const categories = ref<string[]>([]);

const doc = ref<any>({
  title: "",
  content: "",
  category: "",
  tags: "",
  status: "draft",
});

// Simple markdown to HTML conversion
const renderedContent = computed(() => {
  let md = doc.value.content || "";
  // Headers
  md = md.replace(/^### (.+)$/gm, "<h4>$1</h4>");
  md = md.replace(/^## (.+)$/gm, "<h3>$1</h3>");
  md = md.replace(/^# (.+)$/gm, "<h2>$1</h2>");
  // Bold and italic
  md = md.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  md = md.replace(/\*(.+?)\*/g, "<em>$1</em>");
  // Inline code
  md = md.replace(/`([^`]+)`/g, "<code>$1</code>");
  // Links
  md = md.replace(/\[([^\]]+)\]\(([^)]+)\)/g, "<a href=\"$2\" target=\"_blank\">$1</a>");
  // Code blocks
  md = md.replace(/```(\w*)\n([\s\S]*?)```/g, "<pre><code>$2</code></pre>");
  // Line breaks
  md = md.replace(/\n\n/g, "<br><br>");
  md = md.replace(/\n/g, "<br>");
  return md;
});

async function loadCategories() {
  try {
    const res = await documentApi.categories();
    categories.value = res.data?.categories || [];
  } catch { /* ignore */ }
}

async function loadDoc() {
  if (isNew.value) return;
  try {
    const res = await documentApi.get(route.params.id as string);
    doc.value = res.data;
  } catch {
    ElMessage.error("加载失败");
    router.back();
  }
}

async function doSave() {
  try {
    if (isNew.value) {
      await documentApi.create(doc.value);
      ElMessage.success("已创建");
      router.back();
    } else {
      await documentApi.update(route.params.id as string, doc.value);
      ElMessage.success("已保存");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.detail || "保存失败");
  }
}

async function doPublish() {
  try {
    // Save first
    if (isNew.value) {
      const res = await documentApi.create(doc.value);
      doc.value.id = res.data.id;
    } else {
      await documentApi.update(route.params.id as string, doc.value);
    }
    await documentApi.publish(doc.value.id);
    doc.value.status = "published";
    ElMessage.success("已发布到知识库");
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.detail || "发布失败");
  }
}

async function doUnpublish() {
  try {
    await documentApi.unpublish(doc.value.id);
    doc.value.status = "draft";
    ElMessage.success("已撤销发布");
  } catch { ElMessage.error("撤销失败"); }
}

function goBack() {
  router.push("/documents");
}

onMounted(() => {
  loadCategories();
  loadDoc();
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
  margin-bottom: 24px;

  .head-left { 
    display: flex; 
    align-items: center; 
    gap: 14px;

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
}

.editor-wrap { 
  display: flex; 
  gap: 16px; 
  margin-top: 12px;
}

.editor-pane, .preview-pane { 
  flex: 1; 
  border: 1px solid $card-border; 
  border-radius: $radius-lg; 
  overflow: hidden;
  box-shadow: $card-shadow;

  &:hover {
    box-shadow: $card-shadow-hover;
  }
}

.pane-header { 
  padding: 10px 16px; 
  background: #F9FAFB; 
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary; 
  border-bottom: 1px solid $card-border; 
}

.editor-textarea { 
  :deep(textarea) { 
    border: none; 
    border-radius: 0; 
    resize: none; 
    font-family: 'Menlo', 'Consolas', monospace; 
    font-size: 13px; 
    line-height: 1.7;
    background: #FFFFFF;
  } 
}

.preview-content { 
  padding: 16px; 
  min-height: 400px; 
  font-size: 14px; 
  line-height: 1.8; 
  overflow-y: auto; 
  max-height: 560px;
  background: #FFFFFF;

  :deep(h2) { 
    font-size: 20px; 
    margin: 16px 0 10px; 
    color: $text-primary;
    font-weight: 600;
  }
  :deep(h3) { 
    font-size: 17px; 
    margin: 14px 0 8px; 
    color: $text-primary;
    font-weight: 600;
  }
  :deep(h4) { 
    font-size: 15px; 
    margin: 10px 0 6px; 
    color: $text-primary;
    font-weight: 500;
  }
  :deep(code) { 
    background: #F3F4F6; 
    padding: 2px 6px; 
    border-radius: 4px; 
    font-size: 13px;
    color: $accent-red;
  }
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
  :deep(a) { 
    color: $primary-color;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  :deep(p) { margin: 8px 0; }
}

.status-bar { 
  margin-top: 20px; 
  display: flex; 
  align-items: center;
  padding: 14px 16px;
  background: #F9FAFB;
  border-radius: $radius-md;
  border: 1px solid $card-border;
}
</style>
