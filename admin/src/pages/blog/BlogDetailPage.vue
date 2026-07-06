<template>
  <div class="blog-layout">
    <!-- ====== 左侧：文档树 ====== -->
    <div class="left-panel">
      <div class="panel-head">
        <h4>文档导航</h4>
        <el-switch v-model="onlyMine" size="small" active-text="我的" @change="loadTree" />
      </div>
      <div class="tree-wrap">
        <div v-for="node in docTree" :key="node.id"
          :class="['tree-node', { active: curDocId === node.id, 'is-folder': node.docType === 'folder' }]"
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
          <span class="node-title" :class="{ 'is-active': curDocId === node.id }">{{ node.title }}</span>
        </div>
        <div v-if="docTree.length === 0" class="empty-tree">暂无文档</div>
      </div>
    </div>

    <!-- ====== 右侧：文章详情 ====== -->
    <div class="center-panel">
      <div v-if="doc.title">
        <el-button text class="back-btn" @click="$router.push('/blog')">
          <el-icon :size="16"><ArrowLeft /></el-icon> 返回博客列表
        </el-button>
        <h1 class="bd-title">{{ doc.title }}</h1>
        <div class="bd-meta">
          <span v-if="doc.category">{{ doc.category }}</span>
          <span>{{ doc.updated_at?.substring(0,10) }}</span>
        </div>
        <MdPreview :modelValue="doc.content || ''" language="zh-CN" preview-theme="github" :no-highlight="true" class="bd-content" />
      </div>
      <div v-else class="empty-detail">
        <el-empty description="文章不存在" />
      </div>

      <!-- 评论 -->
      <div class="comments-section" v-if="doc.id">
        <h3>评论 ({{ comments.length }})</h3>
        <div class="comment-item" v-for="c in comments" :key="c.id">
          <div class="ci-head"><strong>{{ c.author_name }}</strong> <span>{{ c.created_at?.substring(0,16) }}</span></div>
          <div class="ci-body">{{ c.content }}</div>
          <div class="ci-reply" v-for="r in c.replies" :key="r.id" style="margin-left:32px;">
            <div class="ci-head"><strong>{{ r.author_name }}</strong> <span>{{ r.created_at?.substring(0,16) }}</span></div>
            <div class="ci-body">{{ r.content }}</div>
          </div>
          <el-button size="small" text type="primary" @click="replyingTo=c.id; replyAuthor=''; replyContent=''">回复</el-button>
          <div v-if="replyingTo===c.id" style="margin-top:8px;display:flex;gap:8px;">
            <el-input v-model="replyAuthor" size="small" placeholder="昵称" style="width:100px" />
            <el-input v-model="replyContent" size="small" placeholder="回复..." />
            <el-button size="small" type="primary" @click="submitReply(c.id)">发送</el-button>
          </div>
        </div>
        <div class="comment-form">
          <el-input v-model="myName" size="small" placeholder="昵称" style="width:140px;margin-bottom:8px;" />
          <el-input v-model="myComment" type="textarea" :rows="3" placeholder="写评论..." />
          <el-button size="small" type="primary" style="margin-top:8px;" @click="submitComment">发表评论</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Folder, Document, ArrowDown, ArrowRight, ArrowLeft } from "@element-plus/icons-vue";
import http from "@/utils/request";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

const route = useRoute();
const router = useRouter();
const curDocId = computed(() => route.params.id as string);

// ===== 文档树 =====
interface DocItem { id: string; title: string; parentId?: string; docType?: string; expanded?: boolean; level?: number; }
const docs = ref<DocItem[]>([]);

function normalizeDoc(d: any): DocItem {
  return { ...d, parentId: d.parent_id ?? d.parentId ?? null, docType: d.doc_type ?? d.docType ?? 'document' };
}

const onlyMine = ref(false);
function getMyUserId() {
  try { return JSON.parse(localStorage.getItem("admin_info") || "{}").id || ""; } catch { return ""; }
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

const docTree = computed(() => {
  const map = new Map<string, DocItem[]>();
  for (const d of docs.value) {
    const pid = d.parentId || "root";
    if (!map.has(pid)) map.set(pid, []);
    map.get(pid)!.push({ ...d, expanded: d.docType === 'folder' });
  }
  function hasDoc(folderId: string): boolean {
    return (map.get(folderId) || []).some(c => c.docType !== 'folder' || hasDoc(c.id));
  }
  const result: DocItem[] = [];
  function walk(pid: string, level: number) {
    for (const c of (map.get(pid) || [])) {
      if (c.docType === 'folder' && !hasDoc(c.id)) continue;
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
    router.push('/blog');
  } else {
    router.push(`/blog/${node.id}`);
  }
}

// ===== 文章详情 =====
const doc = ref<any>({});
const comments = ref<any[]>([]);
const myName = ref("");
const myComment = ref("");
const replyingTo = ref("");
const replyAuthor = ref("");
const replyContent = ref("");

async function loadComments() {
  try {
    const res = await http.get(`/public/documents/${curDocId.value}/comments`);
    comments.value = res.data || [];
  } catch { /* */ }
}

async function submitComment() {
  if (!myComment.value.trim()) return;
  const name = myName.value.trim() || "匿名";
  await http.post(`/public/documents/${curDocId.value}/comments`, { author_name: name, content: myComment.value });
  myComment.value = "";
  loadComments();
}

async function submitReply(parentId: string) {
  if (!replyContent.value.trim()) return;
  const name = replyAuthor.value.trim() || "匿名";
  await http.post(`/public/documents/${curDocId.value}/comments`, { author_name: name, content: replyContent.value, parent_id: parentId });
  replyContent.value = "";
  replyingTo.value = "";
  loadComments();
}

async function loadDoc() {
  try {
    const res = await http.get(`/public/documents/${curDocId.value}`);
    doc.value = res.data;
  } catch { doc.value = {}; }
  comments.value = [];
  loadComments();
}

watch(curDocId, () => { if (curDocId.value) loadDoc(); });

onMounted(async () => {
  await loadTree();
  loadDoc();
});
</script>

<style lang="scss" scoped>
.blog-layout {
  display: flex;
  height: 100%;
  margin: -24px;
}

// ===== Left Panel (树) =====
.left-panel {
  width: 260px; min-width: 220px;
  background: #FAFBFC;
  border-right: 1px solid #E5E7EB;
  display: flex; flex-direction: column; overflow: hidden;
  .panel-head {
    display: flex; align-items: center; justify-content: space-between;
    padding: 14px 16px; border-bottom: 1px solid #E5E7EB;
    h4 { margin: 0; font-size: 15px; font-weight: 700; color: #303133; }
    :deep(.el-switch) { flex-shrink: 0; .el-switch__label { font-size: 11px; } }
  }
  .tree-wrap { flex: 1; overflow-y: auto; padding: 4px 0; }
  .empty-tree { text-align: center; color: #909399; font-size: 13px; padding: 40px 20px; }
}

.tree-node {
  display: flex; align-items: center; padding: 6px 10px; font-size: 14px;
  cursor: pointer; color: #303133; transition: background 0.15s; user-select: none;
  &:hover { background: #EBEDF0; }
  &.active { background: #E8EAED; }
  &.is-folder { font-weight: 500; }
  .node-arrow { width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: #909399; }
  .node-icon { width: 22px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: #909399; margin-right: 2px; }
  .node-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    &.is-active { color: #4D6BFE; font-weight: 600; }
  }
}

// ===== Center Panel (文章) =====
.center-panel {
  flex: 1; overflow-y: auto; padding: 24px 32px; background: #F7F8FA;
}

.empty-detail { margin-top: 120px; }

.back-btn { margin-bottom: 16px; color: #4D6BFE; padding: 0; &:hover { opacity: 0.8; } }
.bd-title { font-size: 28px; margin: 0 0 12px; }
.bd-meta { font-size: 13px; color: #909399; margin-bottom: 24px; display: flex; gap: 16px; }
.bd-content { font-size: 15px; line-height: 1.8; color: #303133; background: transparent; border: none; box-shadow: none; }

.comments-section { margin-top: 48px; border-top: 1px solid #ebeef5; padding-top: 24px; max-width: 800px; }
.comment-item { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.ci-head { font-size: 13px; margin-bottom: 4px; span { color: #909399; margin-left: 8px; } }
.ci-body { font-size: 14px; color: #303133; line-height: 1.6; }
.comment-form { margin-top: 24px; }
</style>
