<template>
  <div class="page-wrap" v-loading="loading">
    <el-page-header @back="router.back()" title="返回" style="margin-bottom: 20px">
      <template #content>用户详情</template>
    </el-page-header>

    <el-descriptions :column="2" border v-if="user">
      <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
      <el-descriptions-item label="登录方式">
        <el-tag size="small" :type="user.wechat_openid ? 'success' : 'info'">
          {{ user.wechat_openid ? '微信登录' : '手机号登录' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="手机号">
        <span :style="{ color: user.phone ? '' : '#ccc' }">{{ user.phone || '未绑定' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="微信 OpenID">
        <span v-if="user.wechat_openid" style="font-size:13px;font-family:monospace">{{ user.wechat_openid }}</span>
        <span v-else style="color:#ccc">未绑定</span>
      </el-descriptions-item>
      <el-descriptions-item label="昵称">{{ user.nickname || '—' }}</el-descriptions-item>
      <el-descriptions-item label="注册时间">{{ user.created_at }}</el-descriptions-item>
    </el-descriptions>

    <h4 style="margin: 24px 0 12px">卡片组 ({{ user?.groups?.length || 0 }})</h4>
    <el-table :data="user?.groups || []" stripe>
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <span style="color: #4C6EF5; cursor: pointer" @click="router.push(`/groups/${row.id}`)">
            {{ row.title || '未命名' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="source_text" label="原文" min-width="200" show-overflow-tooltip />
      <el-table-column prop="card_count" label="卡片数" width="100" align="center" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { userApi } from "@/api";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const user = ref<any>(null);

onMounted(async () => {
  loading.value = true;
  try {
    user.value = (await userApi.detail(route.params.id as string)).data;
  } catch { /* handled */ }
  loading.value = false;
});
</script>

<style lang="scss" scoped>
</style>
