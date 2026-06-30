<template>
  <div class="page-wrap" v-loading="loading">
    <el-page-header @back="router.back()" title="返回" style="margin-bottom: 20px">
      <template #content>卡片组详情</template>
    </el-page-header>

    <el-descriptions :column="2" border v-if="group">
      <el-descriptions-item label="标题">{{ group.title || '未命名' }}</el-descriptions-item>
      <el-descriptions-item label="用户">{{ group.user_nickname }} ({{ group.user_phone }})</el-descriptions-item>
      <el-descriptions-item label="卡片数">{{ group.cards?.length || 0 }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ group.created_at }}</el-descriptions-item>
      <el-descriptions-item label="原文" :span="2">{{ group.source_text }}</el-descriptions-item>
    </el-descriptions>

    <h4 style="margin: 24px 0 12px">卡片列表</h4>
    <el-table :data="group?.cards || []" stripe>
      <el-table-column prop="word" label="单词" width="160" />
      <el-table-column prop="general_meaning" label="释义" min-width="200" show-overflow-tooltip />
      <el-table-column prop="pos" label="词性" width="100" align="center" />
      <el-table-column prop="pronunciation" label="发音" width="160" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { groupApi } from "@/api";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const group = ref<any>(null);

onMounted(async () => {
  loading.value = true;
  try {
    group.value = (await groupApi.detail(route.params.id as string)).data;
  } catch { /* handled */ }
  loading.value = false;
});
</script>

<style lang="scss" scoped>
</style>
