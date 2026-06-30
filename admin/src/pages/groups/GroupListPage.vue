<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>卡片组管理</h3>
      <el-input v-model="keyword" placeholder="搜索标题" clearable style="width:260px" @input="loadData" />
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column label="标题" min-width="200">
        <template #default="{ row }">
          <span style="color: #4C6EF5; cursor: pointer" @click="router.push(`/groups/${row.id}`)">
            {{ row.title || '未命名' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="user_nickname" label="用户" width="150" />
      <el-table-column prop="card_count" label="卡片数" width="100" align="center" />
      <el-table-column prop="source_text" label="原文" min-width="200" show-overflow-tooltip />
      <el-table-column prop="created_at" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确定删除该卡片组？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" size="small" text>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="page-footer">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { groupApi } from "@/api";
import { ElMessage } from "element-plus";

const router = useRouter();
const loading = ref(false);
const items = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref("");

async function loadData() {
  loading.value = true;
  try {
    const res = await groupApi.list({ page: page.value, pageSize: pageSize.value, keyword: keyword.value });
    items.value = res.data.items || [];
    total.value = res.data.total || 0;
  } catch { /* handled */ }
  loading.value = false;
}

async function handleDelete(id: string) {
  try {
    await groupApi.delete(id);
    ElMessage.success("已删除");
    loadData();
  } catch { /* handled */ }
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
</style>
