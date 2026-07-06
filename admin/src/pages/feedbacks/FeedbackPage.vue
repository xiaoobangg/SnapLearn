<template>
  <div class="feedback-page">
    <div class="page-head">
      <h2>用户反馈</h2>
    </div>

    <div class="feedback-list" v-loading="loading">
      <div v-if="items.length === 0 && !loading" class="empty">
        <el-empty description="暂无反馈" />
      </div>
      <div class="fb-card" v-for="item in items" :key="item.id">
        <div class="fb-header">
          <span class="fb-time">{{ item.created_at?.substring(0, 16) }}</span>
          <el-tag :type="item.status === 'replied' ? 'success' : 'warning'" size="small">
            {{ item.status === 'replied' ? '已回复' : '待处理' }}
          </el-tag>
        </div>
        <div class="fb-content">{{ item.content }}</div>

        <div class="fb-replies" v-if="item.replies?.length">
          <div class="reply-item" v-for="r in item.replies" :key="r.created_at">
            <span class="reply-label">回复：</span>{{ r.content }}
            <span class="reply-time">{{ r.created_at?.substring(0, 16) }}</span>
          </div>
        </div>

        <div class="fb-reply-box">
          <el-input v-model="replyTexts[item.id]" size="small" placeholder="输入回复..." @keyup.enter="doReply(item)" />
          <el-button size="small" type="primary" @click="doReply(item)" :disabled="!replyTexts[item.id]?.trim()">回复</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import http from "@/utils/request";

const items = ref<any[]>([]);
const loading = ref(false);
const replyTexts = reactive<Record<string, string>>({});

async function load() {
  loading.value = true;
  try {
    const res = await http.get("/admin/feedbacks");
    items.value = res.data || [];
  } catch { /* */ }
  loading.value = false;
}

async function doReply(item: any) {
  const content = (replyTexts[item.id] || "").trim();
  if (!content) return;
  try {
    await http.post(`/admin/feedbacks/${item.id}/reply`, { content });
    ElMessage.success("已回复");
    replyTexts[item.id] = "";
    load();
  } catch { ElMessage.error("回复失败"); }
}

onMounted(load);
</script>

<style lang="scss" scoped>
.feedback-page { padding: 24px; }
.page-head { margin-bottom: 20px; h2 { margin: 0; font-size: 20px; } }
.feedback-list { max-width: 800px; }
.empty { padding: 60px 0; }

.fb-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.fb-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.fb-time { font-size: 13px; color: #909399; }
.fb-content { font-size: 14px; color: #303133; line-height: 1.6; white-space: pre-wrap; }

.fb-replies {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.reply-item {
  font-size: 13px; color: #606266; padding: 6px 0;
  .reply-label { font-weight: 600; color: #4D6BFE; }
  .reply-time { color: #909399; margin-left: 12px; font-size: 12px; }
}

.fb-reply-box {
  display: flex; gap: 8px; margin-top: 12px;
  .el-input { flex: 1; }
}
</style>
