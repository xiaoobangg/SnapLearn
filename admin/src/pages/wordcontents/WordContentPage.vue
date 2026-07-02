<template>
  <div class="word-content-page">
    <div class="page-header">
      <h2>单词内容管理</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索单词..."
          clearable
          style="width: 240px"
          @input="onSearch"
        />
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="word_text" label="单词" width="140" />
      <el-table-column prop="general_meaning" label="释义" min-width="160" show-overflow-tooltip />
      <el-table-column prop="pos" label="词性" width="80" />
      <el-table-column prop="pronunciation" label="发音" width="120" />
      <el-table-column prop="extended_meaning" label="延伸义" min-width="140" show-overflow-tooltip />
      <el-table-column prop="example_sentence" label="例句" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="warning"
            size="small"
            :loading="refreshing === row.word_id"
            @click="handleRefresh(row.word_id)"
          >
            刷新
          </el-button>
        </template>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onMounted } from "vue";
import { wordContentApi } from "@/api";
import { ElMessage } from "element-plus";

const loading = ref(false);
const tableData = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const keyword = ref("");
const refreshing = ref<string | null>(null);

let searchTimeout: ReturnType<typeof setTimeout> | null = null;

onMounted(() => {
  loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const res = await wordContentApi.list({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    });
    const data = res as any;
    tableData.value = data.data?.items || data.data || [];
    total.value = data.data?.total || 0;
  } catch { /* handled */ }
  loading.value = false;
}

function onSearch() {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    page.value = 1;
    loadData();
  }, 300);
}

async function handleRefresh(wordId: string) {
  refreshing.value = wordId;
  try {
    await wordContentApi.refresh(wordId);
    ElMessage.success("刷新成功");
    loadData();
  } catch {
    ElMessage.error("刷新失败");
  } finally {
    refreshing.value = null;
  }
}
</script>

<style lang="scss" scoped>
.word-content-page {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #E5E7EB;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h2 {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      color: #1F2937;
      display: flex;
      align-items: center;
      gap: 12px;

      &::before {
        content: "";
        width: 4px;
        height: 20px;
        background: linear-gradient(180deg, #4D6BFE, #8B5CF6);
        border-radius: 2px;
      }
    }
  }
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #E5E7EB;
  }
}
</style>
