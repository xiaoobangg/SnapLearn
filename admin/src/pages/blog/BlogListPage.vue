<template>
  <div class="blog-layout">
    <!-- ====== 左侧：文档树 ====== -->
    <div class="left-panel">
      <div class="panel-head">
        <h4>文档导航</h4>
        <el-switch v-if="isLoggedIn" v-model="onlyMine" size="small" active-text="我的" @change="onOnlyMineChange" />
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
    <BlogChatPanel />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Folder, Document, ArrowDown, ArrowRight } from "@element-plus/icons-vue";
import BlogChatPanel from "@/components/BlogChatPanel.vue";
import http from "@/utils/request";

const router = useRouter();

// ===== 文档树 =====
interface DocItem { id: string; title: string; category: string; status: string; visibility?: string; parentId?: string; docType?: string; expanded?: boolean; level?: number; }
const docs = ref<DocItem[]>([]);
const selFolder = ref("");
const selFolderTitle = ref("");
const onlyMine = ref(false);
const isLoggedIn = ref(!!localStorage.getItem("admin_token"));

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
  border-right: 1px solid $card-border;
  display: flex;
  flex-direction: column;
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
      font-weight: 700; 
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
    :deep(.el-switch) { flex-shrink: 0; .el-switch__label { font-size: 11px; } }
  }
  .tree-wrap {
    flex: 1;
    overflow-y: auto;
    padding: 4px 0;
  }
  .empty-tree {
    text-align: center;
    color: $text-muted;
    font-size: 13px;
    padding: 40px 20px;
  }
}

.tree-node {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  color: $text-primary;
  transition: all $transition-fast;
  user-select: none;
  border-radius: $radius-sm;
  margin: 0 4px;

  &:hover { background: $sidebar-hover; }
  &.active { 
    background: $sidebar-active; 
    font-weight: 600; 
    color: $primary-color;
  }
  &.is-folder { font-weight: 500; }

  .node-arrow {
    width: 18px; height: 18px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0; color: $text-muted;
    transition: color $transition-fast;
  }
  .node-icon {
    width: 22px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0; color: $text-muted; margin-right: 6px;
    transition: color $transition-fast;
  }
  .node-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .node-count {
    font-size: 11px;
    color: $text-muted;
    background: $card-border;
    border-radius: 10px;
    padding: 0 6px;
    margin-left: 8px;
    flex-shrink: 0;
  }
}

// ===== Center Panel =====
.center-panel {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  background: $bg-gradient-start;
}

.blog-head {
  margin-bottom: 28px;
  h1 { 
    margin: 0; 
    font-size: 24px; 
    font-weight: 700; 
    color: $text-primary;
    display: flex;
    align-items: center;
    gap: 12px;
    &::before {
      content: "";
      width: 4px;
      height: 24px;
      background: linear-gradient(180deg, $primary-color, $accent-purple);
      border-radius: 2px;
    }
  }
}

.blog-list { min-height: 200px; }

.blog-card {
  padding: 24px;
  margin-bottom: 16px;
  background: $card-bg;
  border-radius: $radius-lg;
  border: 1px solid $card-border;
  box-shadow: $card-shadow;
  cursor: pointer;
  transition: all $transition-normal;
  &:hover { 
    box-shadow: $card-shadow-hover; 
    transform: translateY(-1px);
  }
  h3 { 
    margin: 0 0 10px; 
    font-size: 18px; 
    font-weight: 600;
    color: $text-primary;
    transition: color $transition-fast;
  }
  &:hover h3 {
    color: $primary-color;
  }
}

.bc-meta { 
  display: flex; 
  gap: 16px; 
  font-size: 13px; 
  color: $text-muted; 
  margin-bottom: 12px;
  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}
.bc-summary { 
  font-size: 14px; 
  color: $text-secondary; 
  line-height: 1.7; 
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.blog-pager { margin-top: 28px; display: flex; justify-content: center; }

</style>
