<template>
  <div class="word-bank-page">
    <div class="page-header">
      <h2>词库管理</h2>
      <el-button type="primary" @click="showCreate = true">新建词库</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'preset' ? 'success' : ''" size="small">
            {{ row.type === 'preset' ? '预置' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="created_by" label="创建者" width="140" />
      <el-table-column prop="created_at" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="loadData"
      />
    </div>

    <!-- 新建词库对话框 -->
    <el-dialog v-model="showCreate" title="新建词库" width="420px" destroy-on-close>
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="输入词库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :disabled="!createForm.name.trim()" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { onMounted } from "vue";
import { wordBankApi } from "@/api";
import { ElMessage } from "element-plus";

const loading = ref(false);
const tableData = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);

const showCreate = ref(false);
const createForm = reactive({ name: "", description: "" });

onMounted(() => {
  loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const res = await wordBankApi.list({ page: page.value, pageSize: pageSize.value });
    const data = res as any;
    tableData.value = data.data?.items || data.data || [];
    total.value = data.data?.total || 0;
  } catch { /* handled */ }
  loading.value = false;
}

async function handleCreate() {
  if (!createForm.name.trim()) return;
  try {
    await wordBankApi.create(createForm.name.trim(), createForm.description.trim() || undefined);
    ElMessage.success("创建成功");
    showCreate.value = false;
    createForm.name = "";
    createForm.description = "";
    loadData();
  } catch { /* handled */ }
}

function formatTime(t: string) {
  if (!t) return "";
  return new Date(t).toLocaleString("zh-CN");
}
</script>

<style lang="scss" scoped>
.word-bank-page {
  background: rgba(30, 41, 59, 0.95);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h2 { 
      font-size: 18px; 
      font-weight: 600; 
      margin: 0;
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
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }
}
</style>
