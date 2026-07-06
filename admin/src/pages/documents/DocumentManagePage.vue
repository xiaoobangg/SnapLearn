<template>
  <div class="doc-layout">
    <!-- ====== 左侧：文档树 (语雀风格) ====== -->
    <div class="left-panel">
      <div class="panel-head">
        <h4>文档库</h4>
        <div class="panel-actions">
          <el-button size="small" text title="导入 MD 文件" @click.stop="importVisible = true"><el-icon><Upload /></el-icon></el-button>
          <el-button size="small" text title="新建" @click.stop="showRootCreateMenu"><el-icon><Plus /></el-icon></el-button>
        </div>
      </div>
      <div class="search-box">
        <el-input v-model="searchText" size="small" placeholder="搜索文档..." clearable />
      </div>
      <div class="tree-wrap" @contextmenu.prevent="onTreeContextMenu">
        <!-- 行内创建行：根级别 -->
        <div v-if="creating && !creating.parentId && !searchText" class="tree-node creating-row"
          :style="{ paddingLeft: '12px' }">
          <span class="node-arrow" />
          <span class="node-icon">
            <el-icon v-if="creating.type === 'folder'" :size="15"><Folder /></el-icon>
            <el-icon v-else :size="15"><Document /></el-icon>
          </span>
          <el-input v-model="creatingTitle" size="small" class="node-edit-input"
            :placeholder="creating.type === 'folder' ? '文件夹名称' : '文档名称'"
            @keyup.enter="confirmCreate" @keyup.escape="cancelCreate"
            @blur="onCreateBlur" ref="creatingInputRef" />
        </div>

        <template v-for="node in filteredTree" :key="node.id">
          <div
            :class="['tree-node', {
              active: currentDoc?.id === node.id,
              'is-folder': node.docType === 'folder',
              'just-created': node.id === justCreatedId,
              'drag-over': dragOverId === node.id && node.docType === 'folder',
            }]"
            :style="{ paddingLeft: (node.level || 0) * 18 + 12 + 'px' }"
            :draggable="editingNode !== node.id"
            @click="onNodeClick(node)"
            @contextmenu.stop.prevent="showCtxMenu($event, node)"
            @dragstart="onDragStart($event, node)"
            @dragover.prevent="onDragOver($event, node)"
            @dragleave="onDragLeave(node)"
            @drop.prevent="onDrop($event, node)"
          >
            <!-- 展开/折叠箭头 -->
            <span class="node-arrow" v-if="node.docType === 'folder'" @click.stop="toggleGroup(node)">
              <el-icon :size="12"><ArrowDown v-if="node.expanded" /><ArrowRight v-else /></el-icon>
            </span>
            <span class="node-arrow" v-else />
            <!-- 图标 -->
            <span class="node-icon">
              <el-icon v-if="node.docType === 'folder'" :size="15"><Folder /></el-icon>
              <el-icon v-else :size="15"><Document /></el-icon>
            </span>
            <!-- 内联编辑 / 名称 -->
            <template v-if="editingNode === node.id">
              <el-input v-model="editingTitle" size="small" class="node-edit-input"
                @keyup.enter="confirmRename(node)" @blur="confirmRename(node)"
                ref="renameInputRef" />
            </template>
            <template v-else>
              <span class="node-title" :class="{ 'is-published': node.docType !== 'folder' && node.status === 'published', 'is-shared': node.visibility === 'shared' }">
                {{ node.title }}
                <el-icon v-if="node.docType !== 'folder' && node.status === 'published' && node.visibility === 'shared'" :size="12" class="shared-dot"><Share /></el-icon>
              </span>
            </template>
            <!-- hover 显示操作按钮 -->
            <span class="node-tail">
              <el-icon v-if="node.docType === 'folder'" class="tail-icon" @click.stop="showPlusMenu($event, node)">
                <Plus />
              </el-icon>
              <el-icon class="tail-icon tail-more" @click.stop="showCtxMenu($event, node)">
                <MoreFilled />
              </el-icon>
            </span>
          </div>

          <!-- 行内创建行：文件夹子级 -->
          <div v-if="creating && creating.parentId === node.id && node.docType === 'folder' && !searchText"
            class="tree-node creating-row"
            :style="{ paddingLeft: ((node.level || 0) + 1) * 18 + 12 + 'px' }">
            <span class="node-arrow" />
            <span class="node-icon">
              <el-icon v-if="creating.type === 'folder'" :size="15"><Folder /></el-icon>
              <el-icon v-else :size="15"><Document /></el-icon>
            </span>
            <el-input v-model="creatingTitle" size="small" class="node-edit-input"
              :placeholder="creating.type === 'folder' ? '文件夹名称' : '文档名称'"
              @keyup.enter="confirmCreate" @keyup.escape="cancelCreate"
              @blur="onCreateBlur" ref="creatingInputRef" />
          </div>

          <!-- 空文件夹占位 -->
          <div v-if="node.docType === 'folder' && node.expanded && !hasChildren(node.id) && !(creating && creating.parentId === node.id)"
            class="tree-node empty-folder-hint"
            :style="{ paddingLeft: ((node.level || 0) + 1) * 18 + 12 + 'px' }">
            <span class="node-arrow" />
            <span class="node-icon" />
            <span class="node-title hint-text">暂无文档</span>
          </div>
        </template>

        <div v-if="filteredTree.length === 0 && !creating" class="empty-tree">
          {{ searchText ? '无匹配文档' : '点击上方 + 新建文档' }}
        </div>
      </div>
    </div>

    <!-- ====== 右键菜单 ====== -->
    <Teleport to="body">
      <div v-if="ctxVisible" class="yuque-ctx-menu" :style="{ left: ctxX + 'px', top: ctxY + 'px' }" @click.stop>
        <div class="ctx-item" @click="ctxAction('newDoc')"><el-icon :size="14"><Document /></el-icon>新建文档<span class="ctx-shortcut">Ctrl+N</span></div>
        <div class="ctx-item" v-if="ctxNode?.docType === 'folder'" @click="ctxAction('newFolder')"><el-icon :size="14"><FolderAdd /></el-icon>新建子文件夹<span class="ctx-shortcut">Ctrl+Shift+N</span></div>
        <div class="ctx-item" v-else @click="ctxAction('newSiblingFolder')"><el-icon :size="14"><FolderAdd /></el-icon>新建文件夹<span class="ctx-shortcut">Ctrl+Shift+N</span></div>
        <div class="ctx-divider" />
        <div class="ctx-item" @click="ctxAction('rename')"><el-icon :size="14"><Edit /></el-icon>重命名<span class="ctx-shortcut">F2</span></div>
        <div class="ctx-divider" />
        <div class="ctx-item danger" @click="ctxAction('delete')"><el-icon :size="14"><Delete /></el-icon>删除<span class="ctx-shortcut">Del</span></div>
      </div>
    </Teleport>

    <!-- ====== 新建菜单 (文件夹 + 按钮弹出) ====== -->
    <Teleport to="body">
      <div v-if="plusVisible" class="yuque-ctx-menu" :style="{ left: plusX + 'px', top: plusY + 'px' }" @click.stop>
        <div class="ctx-item" @click="plusAction('newDoc')"><el-icon :size="14"><Document /></el-icon>新建文档</div>
        <div class="ctx-item" @click="plusAction('newFolder')"><el-icon :size="14"><FolderAdd /></el-icon>新建子文件夹</div>
      </div>
    </Teleport>

    <!-- ====== 根级别新建菜单 ====== -->
    <Teleport to="body">
      <div v-if="rootMenuVisible" class="yuque-ctx-menu" :style="{ left: rootMenuX + 'px', top: rootMenuY + 'px' }" @click.stop>
        <div class="ctx-item" @click="rootMenuAction('newDoc')"><el-icon :size="14"><Document /></el-icon>新建文档</div>
        <div class="ctx-item" @click="rootMenuAction('newFolder')"><el-icon :size="14"><FolderAdd /></el-icon>新建文件夹</div>
      </div>
    </Teleport>

    <!-- ====== 中间：文档编辑器 ====== -->
    <div class="center-panel">
      <template v-if="currentDoc && currentDoc.docType !== 'folder'">
        <div class="editor-toolbar">
          <el-input v-model="currentDoc.tags" size="small" placeholder="标签(逗号分隔)" style="width:200px" clearable />
          <div class="toolbar-spacer" />
          <span v-if="autoSaveStatus" class="auto-save-status" :class="{ saving: autoSaveStatus === 'saving' }">
            {{ autoSaveStatus === 'saving' ? '保存中...' : '已自动保存' }}
          </span>
          <el-button size="small" :type="(currentDoc.visibility || 'private') === 'shared' ? 'success' : 'default'" plain @click="toggleVisibility">
            <el-icon :size="14"><Share v-if="(currentDoc.visibility || 'private') === 'shared'" /><Lock v-else /></el-icon>
            {{ (currentDoc.visibility || 'private') === 'shared' ? '已共享' : '私有' }}
          </el-button>
          <el-button size="small" type="primary" @click="doSave" :loading="saving">保存并发布</el-button>
        </div>
        <div class="editor-wrap">
          <MdEditor
            v-model="currentDoc.content"
            language="zh-CN"
            preview-theme="github"
            :preview="false"
            :no-highlight="true"
            :toolbars="mdToolbars"
            :footers="[]"
            :onUploadImg="handleUploadImg"
            class="md-editor"
          />
        </div>
      </template>
      <div v-else class="empty-center">
        <el-empty description="选择或新建文档" />
      </div>
    </div>

    <!-- ====== 右侧：AI 助手 ====== -->
    <div class="right-panel" :class="{ collapsed: !showAiPanel }">
      <div class="panel-head">
        <h4>AI 助手</h4>
        <el-button size="small" text class="panel-toggle" @click="showAiPanel = !showAiPanel">
          <el-icon :size="16"><DArrowRight v-if="showAiPanel" /><DArrowLeft v-else /></el-icon>
        </el-button>
        <div class="ai-toolbar">
          <div class="ai-toolbar-left">
            <el-dropdown trigger="click" @command="onConvCommand" popper-class="conv-dropdown-popper">
              <el-button size="small" style="width:150px;text-align:left;overflow:hidden;text-overflow:ellipsis;">
                {{ currentConvTitle || '选择会话' }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="aiConvs.length === 0" disabled>暂无会话</el-dropdown-item>
                  <el-dropdown-item v-for="c in aiConvs" :key="c.chat_id" :command="{ type: 'select', id: c.chat_id }"
                    :class="{ 'is-active': currentConvId === c.chat_id }">
                    <span class="conv-title">{{ c.title }}</span>
                    <button class="conv-del-btn" @click.stop="delConv(c.chat_id)" title="删除会话">
                      <el-icon :size="14"><Delete /></el-icon>
                    </button>
                  </el-dropdown-item>
                  <el-dropdown-item divided :command="{ type: 'new' }">
                    <el-icon :size="14"><Plus /></el-icon> 新建会话
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <button class="model-toggle-btn" @click="toggleAiModel" :class="{ active: aiModel === 'deepseek' }">
            <span class="model-icon ds-icon">DS</span>
            <span class="model-icon qw-icon">QW</span>
          </button>
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
        <el-input
          v-model="aiInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          resize="none"
          placeholder="输入 @ 引用文档... (Enter 发送, Shift+Enter 换行)"
          @keydown.enter="onInputEnter"
          @keyup="onAiInputKeyup"
          @input="onAiInput"
          :disabled="aiLoading"
        />
          <div v-if="showMention" class="mention-dropdown">
            <div v-if="mentionDocs.length === 0" class="mention-empty">无匹配文档</div>
            <div v-for="d in mentionDocs" :key="d.id" class="mention-item" @mousedown.prevent="selectMention(d)">
              <el-icon :size="14"><Document /></el-icon>
              <span>{{ d.title }}</span>
            </div>
          </div>
          <div v-if="showMention" class="mention-dropdown">
            <div v-if="mentionDocs.length === 0" class="mention-empty">无匹配文档</div>
            <div v-for="d in mentionDocs" :key="d.id" class="mention-item" @click="selectMention(d)">
              <el-icon :size="14"><Document /></el-icon>
              <span>{{ d.title }}</span>
            </div>
          </div>
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
import { ref, computed, onMounted, nextTick, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Upload, UploadFilled, Delete, Edit, Document, Folder, FolderAdd, MoreFilled, ArrowDown, ArrowRight, DArrowRight, DArrowLeft, Share, Lock } from "@element-plus/icons-vue";
import { documentApi } from "@/api";
import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

// ===== 文档树 =====
interface DocItem { id: string; title: string; category: string; status: string; visibility?: string; parentId?: string; docType?: string; expanded?: boolean; level?: number; }
interface CreatingState { parentId: string | null; type: 'document' | 'folder'; }
const docs = ref<DocItem[]>([]);

// ===== 行内创建状态 =====
const creating = ref<CreatingState | null>(null);
const creatingTitle = ref('');
const creatingInputRef = ref<any>(null);
const justCreatedId = ref('');

// 自动聚焦创建输入框
watch(() => creating.value, async (val) => {
  if (val) {
    creatingTitle.value = '';
    await nextTick();
    creatingInputRef.value?.focus();
  }
});

const searchText = ref("");
const docTree = computed(() => {
  const map = new Map<string, DocItem[]>();
  for (const d of docs.value) {
    const pid = d.parentId || "root";
    if (!map.has(pid)) map.set(pid, []);
    map.get(pid)!.push({ ...d, expanded: true });
  }
  const result: DocItem[] = [];
  function walk(pid: string, level: number) {
    const children = map.get(pid) || [];
    for (const c of children) {
      c.level = level;
      result.push(c);
      if (c.expanded && c.docType === 'folder') walk(c.id, level + 1);
    }
  }
  walk("root", 0);
  return result;
});

const filteredTree = computed(() => {
  if (!searchText.value.trim()) return docTree.value;
  const kw = searchText.value.toLowerCase();
  return docTree.value.filter(n => n.title.toLowerCase().includes(kw) || (n.docType === 'folder' && hasMatchingChild(n.id, kw)));
});

function hasMatchingChild(pid: string, kw: string): boolean {
  return docs.value.some(d => d.parentId === pid && (d.title.toLowerCase().includes(kw) || (d.docType === 'folder' && hasMatchingChild(d.id, kw))));
}

function toggleGroup(node: DocItem) { node.expanded = !node.expanded; }
function onNodeClick(node: DocItem) {
  if (node.docType === 'folder') {
    toggleGroup(node);
  } else {
    selectDoc(node);
  }
}

// ===== 右键菜单 =====
const ctxVisible = ref(false);
const ctxX = ref(0);
const ctxY = ref(0);
const ctxNode = ref<DocItem | null>(null);

function showCtxMenu(e: MouseEvent, node: DocItem) {
  ctxNode.value = node;
  const menuW = 170;
  ctxX.value = Math.min(e.clientX, window.innerWidth - menuW);
  ctxY.value = Math.min(e.clientY, window.innerHeight - 200);
  ctxVisible.value = true;
  plusVisible.value = false;
}

function closeCtxMenu() { ctxVisible.value = false; plusVisible.value = false; rootMenuVisible.value = false; }
document.addEventListener('click', closeCtxMenu);

// 树空白区域右键 → 弹出根级创建菜单
function onTreeContextMenu(e: MouseEvent) {
  const target = e.target as HTMLElement;
  if (target.closest('.tree-node')) return; // 节点右键由 showCtxMenu 处理
  rootMenuX.value = Math.min(e.clientX, window.innerWidth - 170);
  rootMenuY.value = Math.min(e.clientY, window.innerHeight - 100);
  rootMenuVisible.value = true;
  ctxVisible.value = false;
  plusVisible.value = false;
}

function ctxAction(action: string) {
  const node = ctxNode.value!;
  ctxVisible.value = false;
  if (action === 'newDoc') startCreate(node.docType === 'folder' ? node.id : (node.parentId || null), 'document');
  else if (action === 'newFolder') startCreate(node.id, 'folder');
  else if (action === 'newSiblingFolder') startCreate(node.parentId || null, 'folder');
  else if (action === 'rename') startRename(node);
  else if (action === 'delete') deleteNode(node);
}

// ===== 文件夹 + 按钮弹出菜单 =====
const plusVisible = ref(false);
const plusX = ref(0);
const plusY = ref(0);
const plusNode = ref<DocItem | null>(null);

function showPlusMenu(e: MouseEvent, node: DocItem) {
  plusNode.value = node;
  const menuW = 160;
  plusX.value = Math.min(e.clientX, window.innerWidth - menuW);
  plusY.value = Math.min(e.clientY, window.innerHeight - 150);
  plusVisible.value = true;
  ctxVisible.value = false;
}

function plusAction(action: string) {
  const node = plusNode.value!;
  plusVisible.value = false;
  if (action === 'newDoc') startCreate(node.id, 'document');
  else if (action === 'newFolder') startCreate(node.id, 'folder');
}

// ===== 根级别新建菜单 =====
const rootMenuVisible = ref(false);
const rootMenuX = ref(0);
const rootMenuY = ref(0);

function showRootCreateMenu(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
  rootMenuX.value = rect.left;
  rootMenuY.value = rect.bottom + 4;
  rootMenuVisible.value = true;
  ctxVisible.value = false;
  plusVisible.value = false;
}

function rootMenuAction(action: string) {
  rootMenuVisible.value = false;
  if (action === 'newDoc') startCreate(null, 'document');
  else if (action === 'newFolder') startCreate(null, 'folder');
}

// ===== 内联重命名 =====
const editingNode = ref("");
const editingTitle = ref("");
const renameInputRef = ref();
function startRename(node: DocItem) { editingNode.value = node.id; editingTitle.value = node.title; }
async function confirmRename(node: DocItem) {
  if (!editingTitle.value.trim()) { editingNode.value = ""; return; }
  try {
    await documentApi.update(node.id, { title: editingTitle.value });
    node.title = editingTitle.value;
    if (currentDoc.value?.id === node.id) currentDoc.value.title = editingTitle.value;
  } catch { /* */ }
  editingNode.value = "";
}

// ===== 删除 =====
async function deleteNode(node: DocItem) {
  try { await ElMessageBox.confirm(`确定删除"${node.title}"？`, "提示", { type: "warning" }); } catch { return; }
  try {
    await documentApi.delete(node.id);
    if (currentDoc.value?.id === node.id) currentDoc.value = null;
    loadDocs();
    ElMessage.success("已删除");
  } catch { ElMessage.error("删除失败"); }
}

// ===== 行内创建 =====
function startCreate(parentId: string | null, type: 'document' | 'folder') {
  // 自动展开父文件夹
  if (parentId) {
    const parent = docs.value.find(d => d.id === parentId);
    if (parent) parent.expanded = true;
  }
  creating.value = { parentId, type };
}

async function confirmCreate() {
  const title = creatingTitle.value.trim();
  if (!title || !creating.value) { cancelCreate(); return; }

  try {
    if (creating.value.type === 'folder') {
      await documentApi.createFolder({ title, parentId: creating.value.parentId });
      ElMessage.success('文件夹已创建');
    } else {
      const res = await documentApi.create({ title, content: '', parentId: creating.value.parentId, visibility: 'private' });
      const newId = res.data?.id;
      await loadDocs();
      if (newId) {
        justCreatedId.value = newId;
        setTimeout(() => { justCreatedId.value = ''; }, 2000);
        const newNode = docs.value.find(d => d.id === newId);
        if (newNode) selectDoc(newNode);
      }
      ElMessage.success('文档已创建，开始编辑');
    }
    if (creating.value?.type === 'folder') await loadDocs();
  } catch (e: any) { ElMessage.error(e?.response?.data?.detail || '创建失败'); }

  creating.value = null;
}

function cancelCreate() {
  creating.value = null;
}

function onCreateBlur() {
  // 延迟以允许 Enter 键先触发 confirmCreate
  setTimeout(() => {
    if (creating.value && !creatingTitle.value.trim()) {
      cancelCreate();
    }
  }, 150);
}

function hasChildren(parentId: string): boolean {
  return docs.value.some(d => d.parentId === parentId);
}

// ===== Markdown 编辑器配置 =====
const mdToolbars: any[] = [
  'bold', 'italic', 'strikethrough', 'title', '-',
  'unorderedList', 'orderedList', 'task', '-',
  'code', 'codeBlock', 'link', 'image', 'table', '-',
  'preview', 'catalog', 'fullscreen',
];

// ===== 拖拽移动 =====
const dragNode = ref<DocItem | null>(null);
const dragOverId = ref('');

function onDragStart(e: DragEvent, node: DocItem) {
  dragNode.value = node;
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move';
  }
}

function onDragOver(_e: DragEvent, node: DocItem) {
  if (!dragNode.value || dragNode.value.id === node.id) return;
  if (node.docType === 'folder') {
    dragOverId.value = node.id;
  }
}

function onDragLeave(node: DocItem) {
  if (dragOverId.value === node.id) {
    dragOverId.value = '';
  }
}

async function onDrop(_e: DragEvent, target: DocItem) {
  dragOverId.value = '';
  const src = dragNode.value;
  dragNode.value = null;
  if (!src || src.id === target.id) return;
  // 只能移到文件夹下
  if (target.docType !== 'folder') return;

  try {
    await documentApi.move(src.id, { parentId: target.id });
    ElMessage.success(`已移动到「${target.title}」`);
    loadDocs();
  } catch { ElMessage.error("移动失败"); }
}

// ===== 当前文档 =====
const currentDoc = ref<any>(null);
const saving = ref(false);
const savedContent = ref('');            // 上次保存时的内容，用于脏检测
const autoSaveStatus = ref('');         // '' | 'saving' | 'saved'
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null;

// ===== 自动保存 (3 秒无操作后触发) =====
watch(() => currentDoc.value?.content, () => {
  if (!currentDoc.value?.id) return;                         // 新文档不自动保存
  if (currentDoc.value.content === savedContent.value) return; // 内容未变化
  if (autoSaveTimer) clearTimeout(autoSaveTimer);
  autoSaveStatus.value = '';
  autoSaveTimer = setTimeout(async () => {
    autoSaveStatus.value = 'saving';
    try {
      await doSaveSilent();
      savedContent.value = currentDoc.value.content;
      autoSaveStatus.value = 'saved';
      setTimeout(() => { if (autoSaveStatus.value === 'saved') autoSaveStatus.value = ''; }, 2000);
    } catch {
      autoSaveStatus.value = '';
    }
  }, 3000);
});

async function doSaveSilent() {
  if (!currentDoc.value.id) return;
  if (!currentDoc.value.visibility) currentDoc.value.visibility = 'private';
  await documentApi.update(currentDoc.value.id, currentDoc.value);
  loadDocs();
}

function markSaved() {
  savedContent.value = currentDoc.value?.content || '';
}

// 标准化 API 返回的 snake_case 字段为 camelCase
function normalizeDoc(d: any): DocItem {
  return {
    ...d,
    parentId: d.parent_id ?? d.parentId ?? null,
    docType: d.doc_type ?? d.docType ?? 'document',
    sortOrder: d.sort_order ?? d.sortOrder ?? 0,
    sourceType: d.source_type ?? d.sourceType,
    sourceName: d.source_name ?? d.sourceName,
    knowledgeFileId: d.knowledge_file_id ?? d.knowledgeFileId,
    createdAt: d.created_at ?? d.createdAt,
    updatedAt: d.updated_at ?? d.updatedAt,
  };
}

async function loadDocs() {
  try {
    const res = await documentApi.tree();
    docs.value = (res.data || []).map((d: any) => ({
      ...normalizeDoc(d),
      expanded: (d.doc_type ?? d.docType) === 'folder',
    }));
  } catch { /* ignore */ }
}

async function selectDoc(doc: DocItem) {
  try {
    const res = await documentApi.get(doc.id);
    currentDoc.value = normalizeDoc(res.data);
    markSaved();
  } catch { ElMessage.error("加载失败"); }
}

async function doSave() {
  saving.value = true;
  try {
    // 确保 visibility 有默认值
    if (!currentDoc.value.visibility) currentDoc.value.visibility = 'private';
    if (!currentDoc.value.id) {
      const res = await documentApi.create(currentDoc.value);
      currentDoc.value.id = res.data.id;
      currentDoc.value.status = res.data.status;
    } else {
      const res = await documentApi.update(currentDoc.value.id, currentDoc.value);
      currentDoc.value.status = res.data.status;
    }
    ElMessage.success("已保存并发布");
    markSaved();
    loadDocs();
  } catch (e: any) { ElMessage.error(e?.response?.data?.detail || "保存失败"); }
  saving.value = false;
}

function toggleVisibility() {
  if (!currentDoc.value) return;
  currentDoc.value.visibility = (currentDoc.value.visibility || 'private') === 'shared' ? 'private' : 'shared';
  if (currentDoc.value.id) doSave();
}

// ===== 图片上传 =====
async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  const results: string[] = [];
  for (const file of files) {
    try {
      const fd = new FormData();
      fd.append('file', file);
      const baseUrl = import.meta.env.VITE_APP_BASE_API || '';
      const token = localStorage.getItem('admin_token') || '';
      const resp = await fetch(`${baseUrl}/admin/documents/upload-image`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: fd,
      });
      const data = await resp.json();
      if (data.url) results.push(data.url);
      else results.push('upload failed');
    } catch {
      results.push('upload failed');
    }
  }
  callback(results);
}

// ===== 批量导入 =====
const importVisible = ref(false);
const importFiles = ref<any[]>([]);
const importing = ref(false);
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

// ===== AI =====
const showAiPanel = ref(true);
const aiInput = ref("");
const aiMessages = ref<{ role: string; content: string; tool?: string }[]>([]);
const aiLoading = ref(false);
const aiMsgs = ref<HTMLElement>();
const aiConvs = ref<any[]>([]);
const currentConvId = ref<string>("");
const isCreatingNew = ref(false);  // 标记是否正在新建会话，避免 loadAiConvs 自动选中最新会话
const aiModel = ref<"deepseek" | "dashscope">("deepseek");

// ===== @ 文档引用 =====
const showMention = ref(false);
const mentionDocs = ref<DocItem[]>([]);
const mentionQuery = ref("");

function onAiInput() {
  const val = aiInput.value;
  const atIdx = val.lastIndexOf("@");
  if (atIdx >= 0 && (atIdx === 0 || val[atIdx - 1] === " ")) {
    mentionQuery.value = val.slice(atIdx + 1).toLowerCase();
    mentionDocs.value = docs.value
      .filter(d => d.docType !== "folder" && d.title.toLowerCase().includes(mentionQuery.value))
      .slice(0, 8);
    showMention.value = true;
  } else {
    showMention.value = false;
  }
}

function onAiInputKeyup(e: KeyboardEvent) {
  if (e.key === "Escape") showMention.value = false;
  if (e.key === "@") {
    mentionQuery.value = "";
    mentionDocs.value = docs.value.filter(d => d.docType !== "folder").slice(0, 8);
    showMention.value = true;
  }
}

// Enter 发送，Shift+Enter 换行
function onInputEnter(e: KeyboardEvent) {
  if (e.shiftKey) return;  // Shift+Enter 换行，保留默认行为
  e.preventDefault();
  sendAi();
}

function selectMention(d: DocItem) {
  const val = aiInput.value;
  const atIdx = val.lastIndexOf("@");
  aiInput.value = val.slice(0, atIdx) + `@${d.title} `;
  showMention.value = false;
}

async function loadAiConvs() {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/chat/conversations`, { headers: { Authorization: `Bearer ${token}` } });
    aiConvs.value = await resp.json();
    // 仅在非新建会话且无当前会话时，默认选中最新会话
    if (aiConvs.value.length > 0 && !currentConvId.value && !isCreatingNew.value) {
      switchConv(aiConvs.value[0].chat_id);
    }
  } catch { /* ignore */ }
}
async function loadAiHistory(chatId: string) {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    const resp = await fetch(`${baseUrl}/api/v1/chat/messages/${chatId}?mode=agent`, { headers: { Authorization: `Bearer ${token}` } });
    const msgs = await resp.json();
    aiMessages.value = msgs.map((m: any) => ({ role: m.role === "user" ? "user" : "assistant", content: m.content }));
  } catch { aiMessages.value = []; }
}
const currentConvTitle = computed(() => {
  if (!currentConvId.value) return "";
  const c = aiConvs.value.find((x: any) => x.chat_id === currentConvId.value);
  return c?.title || "";
});

async function onConvCommand(cmd: any) {
  if (cmd.type === 'select') { switchConv(cmd.id); return; }
  if (cmd.type === 'new') { newAiConv(); return; }
}

async function delConv(chatId: string) {
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    await fetch(`${baseUrl}/api/v1/chat/conversations/${chatId}`, { method: "DELETE", headers: { Authorization: `Bearer ${token}` } });
    if (currentConvId.value === chatId) newAiConv();
    loadAiConvs();
  } catch { /* ignore */ }
}

async function switchConv(chatId: string) { if (!chatId) { newAiConv(); return; } currentConvId.value = chatId; loadAiHistory(chatId); }
async function newAiConv() { 
  isCreatingNew.value = true;   // 标记正在新建，阻止 loadAiConvs 自动选中
  currentConvId.value = ""; 
  aiMessages.value = [{ role: "assistant", content: "你好！我是文档助手。" }]; 
  await loadAiConvs(); 
  isCreatingNew.value = false;  // 恢复标记
}
async function deleteAiConv() {
  if (!currentConvId.value) return;
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || "";
    const token = localStorage.getItem("admin_token") || "";
    await fetch(`${baseUrl}/api/v1/chat/conversations/${currentConvId.value}`, { method: "DELETE", headers: { Authorization: `Bearer ${token}` } });
    newAiConv();
  } catch { /* ignore */ }
}
function toggleAiModel() { aiModel.value = aiModel.value === "deepseek" ? "dashscope" : "deepseek"; }
function scrollAi() { nextTick(() => { if (aiMsgs.value) aiMsgs.value.scrollTop = aiMsgs.value.scrollHeight; }); }

async function sendAi() {
  const msg = aiInput.value.trim();
  if (!msg || aiLoading.value) return;
  aiInput.value = "";
  if (!currentConvId.value) currentConvId.value = Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
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
      body: JSON.stringify({ message: msg, chat_id: currentConvId.value, model: aiModel.value, mode: "agent", agent_type: "document" }),
    });
    const reader = resp.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let isFirstLine = true;
    const last = aiMessages.value[aiMessages.value.length - 1];
    while (reader) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";
      for (const line of lines) {
        if (line.startsWith("data:")) {
          const data = line.slice(5).replace(/^ /, "");
          if (data === "[DONE]") continue;
          if (!isFirstLine) last.content += "\n";
          if (data.startsWith("{")) {
            try { const p = JSON.parse(data); if (p.chat_id) currentConvId.value = p.chat_id; } catch { last.content += data; }
          } else { last.content += data; }
          isFirstLine = false;
        } else if (line === "") {
          isFirstLine = true;
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

// 监听消息变化，自动滚动到底部
watch(aiMessages, () => scrollAi(), { deep: true });
</script>

<style lang="scss" scoped>
.doc-layout {
  display: flex;
  height: 100%;
  min-height: calc(100vh - 120px); // fallback: header(60px) + padding(48px) + buffer
  background: $bg-gradient-start;
}

// ===== 右键菜单 / + 菜单 (语雀风格) =====
.yuque-ctx-menu {
  position: fixed;
  z-index: 9999;
  min-width: 160px;
  background: $card-bg;
  border: 1px solid $card-border;
  border-radius: $radius-md;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 6px 0;
  animation: ctxFadeIn 0.12s ease;

  .ctx-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 16px;
    font-size: 13px;
    color: $text-primary;
    cursor: pointer;
    transition: background $transition-fast;

    &:hover { background: $sidebar-hover; }
    &.danger { 
      color: $accent-red;
      &:hover { background: rgba($accent-red, 0.06); }
    }
    .ctx-shortcut {
      margin-left: auto;
      font-size: 11px;
      color: $text-muted;
      letter-spacing: 0.3px;
    }
  }
  .ctx-divider {
    height: 1px;
    background: $card-border;
    margin: 4px 0;
  }
}
@keyframes ctxFadeIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
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
    .panel-actions {
      display: flex;
      gap: 4px;
    }
  }
  .search-box {
    padding: 10px 12px;
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

// ===== Tree Node =====
.tree-node {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  color: $text-primary;
  transition: all $transition-fast;
  border-radius: $radius-sm;
  margin: 0 4px;
  position: relative;
  user-select: none;

  &:hover {
    background: $sidebar-hover;
    .node-tail { opacity: 1; }
  }
  &.active {
    background: $sidebar-active;
    font-weight: 600;
    color: $primary-color;
    .node-tail { opacity: 1; }
    .node-icon, .node-arrow { color: $primary-color; }
  }
  &.drag-over {
    background: #DBEAFE;
    outline: 2px dashed $primary-color;
    outline-offset: -2px;
  }

  .node-arrow {
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: $text-muted;
    transition: color $transition-fast;
  }
  .node-icon {
    width: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: $text-muted;
    margin-right: 6px;
    transition: color $transition-fast;
  }
  .node-edit-input {
    flex: 1;
    :deep(.el-input__wrapper) {
      padding: 0 8px;
      height: 26px;
      background: transparent;
      box-shadow: none;
      border: 1px solid $primary-color;
    }
  }
  .node-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    &.is-published { color: $accent-green; }
    &.is-shared { color: $accent-green; }
    .shared-dot {
      margin-left: 4px;
      color: $accent-green;
      vertical-align: middle;
    }
  }
  .node-tail {
    opacity: 0;
    transition: opacity $transition-fast;
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;
    margin-left: 4px;

    .tail-icon {
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: $radius-sm;
      color: $text-muted;
      font-size: 15px;
      cursor: pointer;
      transition: all $transition-fast;
      &:hover {
        background: rgba($text-primary, 0.08);
        color: $text-primary;
      }
    }
    .tail-more { font-size: 16px; }
  }
}

// 行内创建行
.creating-row {
  background: #F0F4FF;
  border: 1px dashed $primary-color;
  border-radius: 4px;
  margin: 2px 8px;
  animation: createSlideIn 0.15s ease;

  .node-edit-input {
    flex: 1;
    :deep(.el-input__wrapper) {
      padding: 0 8px;
      height: 26px;
      background: transparent;
      box-shadow: none;
      border: none;
    }
    :deep(.el-input__inner) {
      font-size: 13px;
      &::placeholder { color: #A0AEC0; font-size: 12px; }
    }
  }
}
@keyframes createSlideIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

// 新建节点短暂高亮
.just-created {
  animation: nodeHighlight 1.5s ease;
}
@keyframes nodeHighlight {
  0% { background: #DBEAFE; }
  100% { background: transparent; }
}

// 空文件夹占位
.empty-folder-hint {
  cursor: default;
  pointer-events: none;
  .hint-text {
    color: $text-muted;
    font-size: 12px;
    font-style: italic;
  }
  &:hover { background: transparent; }
}

// ===== Center Panel =====
.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: $card-bg;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: $card-bg;
  border-bottom: 1px solid $card-border;
  .toolbar-spacer { flex: 1; }
  .auto-save-status {
    font-size: 12px;
    color: $text-muted;
    white-space: nowrap;
    &.saving { color: $primary-color; }
  }
}
.editor-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;

  .md-editor {
    flex: 1;
    min-height: 0;
    height: 100%;
    width: 100%;
    border: none;
    border-radius: 0;
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
  transition: width $transition-normal, min-width $transition-normal;

  &.collapsed {
    width: 36px;
    min-width: 36px;
    .ai-toolbar, .ai-messages, .ai-input { display: none; }
    .panel-head {
      padding: 12px 8px;
      h4 { display: none; }
    }
  }

  .panel-head {
    position: relative;
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
    .panel-toggle {
      position: absolute; 
      right: 4px; 
      top: 8px;
    }
    .ai-toolbar { 
      display: flex; 
      align-items: center; 
      justify-content: space-between;
      gap: 8px;
      margin-top: 10px;

      .ai-toolbar-left {
        display: flex;
        align-items: center;
        flex: 1;
      }
    }
    .conv-title { 
      flex: 1; 
      overflow: hidden; 
      text-overflow: ellipsis; 
    }
    :deep(.el-dropdown-menu__item.is-active) { 
      color: $primary-color; 
      font-weight: 600; 
    }
    /* el-dropdown-menu__item 的布局样式见文件末尾非 scoped 块（Teleport 到 body） */

    .model-toggle-btn {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 2px;
      padding: 3px 4px;
      border-radius: 20px;
      background: #F5F7FA;
      border: 1px solid #E4E7ED;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;
      width: 76px;
      height: 28px;
      overflow: hidden;

      &:hover {
        background: #EEF2FF;
        border-color: rgba(77, 107, 254, 0.3);
      }

      .model-icon {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 700;
        color: #909399;
        transition: all 0.2s ease;
        z-index: 1;
        position: relative;
        height: 20px;
        border-radius: 10px;

        &.ds-icon { 
          color: #67C23A; 
        }
        &.qw-icon { 
          color: #E6A23C; 
        }
      }

      &::after {
        content: '';
        position: absolute;
        top: 3px;
        left: 3px;
        width: 34px;
        height: 20px;
        background: #FFFFFF;
        border-radius: 10px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06);
        transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      }

      &.active::after {
        transform: translateX(34px);
      }

      &.active {
        background: linear-gradient(135deg, rgba(77, 107, 254, 0.08), rgba(139, 92, 246, 0.08));
        border-color: rgba(77, 107, 254, 0.25);
      }
    }

    .conv-del-btn {
      flex-shrink: 0;
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: $radius-sm;
      border: none;
      background: transparent;
      color: $text-muted;
      cursor: pointer;
      transition: all $transition-fast;

      &:hover {
        background: rgba($accent-red, 0.08);
        color: $accent-red;
      }

      &:active {
        background: rgba($accent-red, 0.15);
      }
    }
  }
  
  .ai-messages { 
    flex: 1; 
    overflow-y: auto; 
    padding: 12px; 
    background: #F9FAFB; 
  }
  
  .ai-msg {
    margin-bottom: 14px;
    
    &.user {
      text-align: right;
      .msg-content { 
        background: $primary-color; 
        color: #FFF; 
        display: inline-block; 
        padding: 10px 14px; 
        border-radius: $radius-lg; 
        border-bottom-right-radius: 4px; 
        max-width: 85%; 
        text-align: left; 
        font-size: 13px; 
        white-space: pre-wrap; 
        box-shadow: 0 2px 6px rgba(77,107,254,0.2); 
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
        box-shadow: $card-shadow;
        font-size: 13px; 
        white-space: pre-wrap; 
        color: $text-primary; 
        border: 1px solid $card-border;
        &.typing { color: $text-muted; }
      }
    }
  }
  
  .msg-tool { 
    font-size: 12px; 
    color: $accent-green; 
    margin-top: 4px; 
    padding: 4px 10px; 
    background: rgba(16,185,129,0.1); 
    border-radius: $radius-sm; 
    display: inline-block; 
  }
  
  .ai-input {
    display: flex;
    align-items: flex-end;  // 底部对齐，让发送按钮始终在底部
    gap: 8px;
    padding: 12px;
    border-top: 1px solid $card-border;
    background: $card-bg;
    position: relative;

    :deep(.el-textarea) {
      flex: 1;
      .el-textarea__inner {
        min-height: 32px !important;
        max-height: 180px !important;
        padding: 6px 12px;
        font-size: 13px;
        line-height: 1.5;
        border-radius: $radius-md;
        resize: none;
        box-shadow: 0 0 0 1px $card-border inset;
        transition: box-shadow 0.2s ease;

        &:hover {
          box-shadow: 0 0 0 1px rgba($primary-color, 0.4) inset;
        }

        &:focus {
          box-shadow: 0 0 0 1px $primary-color inset;
        }
      }
    }

    .el-button {
      flex-shrink: 0;
      height: 32px;
      min-width: 60px;
    }
  }

  .mention-dropdown {
    position: absolute;
    bottom: 100%;
    left: 0;
    right: 0;
    margin-bottom: 4px;
    background: #fff;
    border: 1px solid #E5E7EB;
    border-radius: 8px;
    box-shadow: 0 -4px 12px rgba(0,0,0,0.08);
    max-height: 200px;
    overflow-y: auto;
    z-index: 100;
  }

  .mention-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    font-size: 13px;
    cursor: pointer;
    color: #303133;
    &:hover { background: #F3F4F6; }
  }

  .mention-empty {
    padding: 12px;
    font-size: 13px;
    color: #909399;
    text-align: center;
  }
}
</style>

<!-- 非 scoped：el-dropdown-menu 通过 Teleport 渲染到 body，scoped 样式无法穿透 -->
<style lang="scss">
.conv-dropdown-popper {
  .el-dropdown-menu__item {
    display: flex !important;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 14px;

    .conv-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
      color: #303133;
    }

    .conv-del-btn {
      flex-shrink: 0;
      margin-left: auto;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      border: none;
      background: transparent;
      color: #909399;
      cursor: pointer;
      transition: all 0.2s ease;
      opacity: 0;
      visibility: hidden;

      &:hover {
        background: rgba(245, 108, 108, 0.1);
        color: #F56C6C;
        opacity: 1;
      }

      &:active {
        background: rgba(245, 108, 108, 0.18);
      }
    }

    &:hover .conv-del-btn {
      opacity: 0.7;
      visibility: visible;
    }

    &.is-active {
      color: #4D6BFE;
      font-weight: 600;
      background: rgba(77, 107, 254, 0.06);

      .conv-title {
        color: #4D6BFE;
      }
    }
  }
}
</style>
