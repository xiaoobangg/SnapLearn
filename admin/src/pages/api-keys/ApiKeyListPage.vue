<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>API Key 管理</h3>
      <el-button type="primary" size="small" @click="openCreate">创建 Key</el-button>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无 API Key">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="prefix" label="前缀" width="120" />
      <el-table-column prop="is_active" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.is_active ? 'success' : 'danger'" size="small">{{ row.is_active ? '启用' : '已撤销' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="last_used_at" label="最后使用" width="170" />
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button v-if="row.is_active" size="small" link type="danger" @click="doRevoke(row)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 dialog -->
    <el-dialog v-model="createVisible" title="创建 API Key" width="500px">
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 Coze 插件" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建结果 -->
    <el-dialog v-model="resultVisible" title="Key 已创建" width="550px">
      <el-alert type="warning" title="此 Key 仅显示一次，请立即复制保存！" :closable="false" show-icon style="margin-bottom:12px" />
      <el-input v-model="newKey" readonly style="margin-bottom:8px">
        <template #append>
          <el-button @click="copyKey">复制</el-button>
        </template>
      </el-input>
      <div style="font-size:12px;color:#868e96">名称: {{ newKeyName }} · 前缀: {{ newKeyPrefix }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/utils/request";

const loading = ref(false);
const items = ref<any[]>([]);

// Create
const createVisible = ref(false);
const form = ref({ name: "" });
const resultVisible = ref(false);
const newKey = ref("");
const newKeyName = ref("");
const newKeyPrefix = ref("");

function openCreate() {
  form.value.name = "";
  createVisible.value = true;
}

async function doCreate() {
  try {
    const res = await http.post("/api-keys", { name: form.value.name });
    newKey.value = res.data.key;
    newKeyName.value = res.data.name;
    newKeyPrefix.value = res.data.prefix;
    createVisible.value = false;
    resultVisible.value = true;
    loadData();
  } catch { ElMessage.error("创建失败"); }
}

async function copyKey() {
  try {
    await navigator.clipboard.writeText(newKey.value);
    ElMessage.success("已复制");
  } catch { ElMessage.error("复制失败"); }
}

async function doRevoke(row: any) {
  try { await ElMessageBox.confirm("确定撤销？", "提示", { type: "warning" }); } catch { return; }
  try {
    await http.delete(`/api-keys/${row.id}`);
    ElMessage.success("已撤销");
    loadData();
  } catch { ElMessage.error("撤销失败"); }
}

async function loadData() {
  loading.value = true;
  try {
    const res = await http.get("/api-keys");
    items.value = res.data || [];
  } catch { /* ignore */ }
  loading.value = false;
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
.page-head { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  margin-bottom: 20px; 
  h3 { 
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
}
</style>
