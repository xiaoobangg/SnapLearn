<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>卡片管理</h3>
      <el-input v-model="keyword" placeholder="搜索单词" clearable style="width:260px" @input="loadData" />
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="word" label="单词" width="160" />
      <el-table-column prop="general_meaning" label="释义" min-width="200" show-overflow-tooltip />
      <el-table-column prop="pos" label="词性" width="100" align="center" />
      <el-table-column prop="user_nickname" label="用户" width="140" />
      <el-table-column prop="created_at" label="创建时间" width="180" />
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
import { cardApi } from "@/api";

const loading = ref(false);
const items = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref("");

async function loadData() {
  loading.value = true;
  try {
    const res = await cardApi.list({ page: page.value, pageSize: pageSize.value, keyword: keyword.value });
    items.value = res.data.items || [];
    total.value = res.data.total || 0;
  } catch { /* handled */ }
  loading.value = false;
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
</style>
