<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>AI 对话日志</h3>
      <div class="filters">
        <el-input v-model="filterUserId" placeholder="按用户ID搜索" clearable style="width:200px;margin-right:8px" @clear="loadData" @keyup.enter="loadData" />
        <el-input v-model="filterChatId" placeholder="按对话ID搜索" clearable style="width:240px;margin-right:8px" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px;margin-right:8px" @change="loadData">
          <el-option label="success" value="success" />
          <el-option label="error" value="error" />
        </el-select>
        <el-input-number v-model="filterMinDuration" placeholder="最小耗时" :min="0" :step="100" controls-position="right" style="width:140px;margin-right:8px" @change="loadData" />
        <span style="margin-right:8px;color:#909399">-</span>
        <el-input-number v-model="filterMaxDuration" placeholder="最大耗时" :min="0" :step="100" controls-position="right" style="width:140px;margin-right:8px" @change="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无对话日志">
      <el-table-column prop="user_id" label="用户" min-width="130" show-overflow-tooltip />
      <el-table-column prop="model" label="模型" min-width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.model === 'dashscope' ? 'warning' : 'primary'">{{ row.model || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="user_message" label="用户消息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="response_text" label="AI 回答" min-width="280" show-overflow-tooltip />
      <el-table-column prop="total_tokens" label="Token" min-width="80" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.total_tokens > 5000 ? '#DC2626' : '#6B7280' }">{{ row.total_tokens || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="duration_ms" label="耗时(ms)" min-width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.duration_ms > 5000 ? '#DC2626' : '#6B7280' }">{{ row.duration_ms }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="时间" min-width="170" />
      <el-table-column label="详情" min-width="70" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showDetail(row)">明细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="对话详情" width="800px" top="5vh">
      <div v-if="detail" class="detail-body">
        <div class="meta-row">
          <div><strong>用户：</strong>{{ detail.user_id }}</div>
          <div><strong>对话：</strong>{{ detail.chat_id }}</div>
          <div><strong>模型：</strong>{{ detail.model }}</div>
          <div><strong>时间：</strong>{{ detail.created_at }}</div>
        </div>
        <div class="meta-row">
          <div><strong>状态：</strong><el-tag size="small" :type="detail.status === 'success' ? 'success' : 'danger'">{{ detail.status }}</el-tag></div>
          <div><strong>耗时：</strong>{{ detail.duration_ms }} ms</div>
          <div><strong>Token：</strong>输入 {{ detail.prompt_tokens || '-' }} / 输出 {{ detail.completion_tokens || '-' }} / 总 {{ detail.total_tokens || '-' }}</div>
        </div>

        <div class="block">
          <div class="block-head">
            <strong>用户消息</strong>
            <el-button size="small" text @click="copyText(detail.user_message)">复制</el-button>
          </div>
          <pre class="block-body">{{ detail.user_message || '(空)' }}</pre>
        </div>

        <div class="block">
          <div class="block-head">
            <strong>AI 回答</strong>
            <el-button size="small" text @click="copyText(detail.response_text)">复制</el-button>
          </div>
          <pre class="block-body">{{ detail.response_text || '(空)' }}</pre>
        </div>

        <div v-if="detail.error_message" class="block">
          <div class="block-head">
            <strong style="color:#F56C6C">错误信息</strong>
          </div>
          <pre class="block-body block-error">{{ detail.error_message }}</pre>
        </div>
      </div>
    </el-dialog>

    <div class="page-footer">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { chatTraceApi } from "@/api";

const items = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filterUserId = ref("");
const filterChatId = ref("");
const filterStatus = ref("");
const filterMinDuration = ref<number | undefined>();
const filterMaxDuration = ref<number | undefined>();

const detailVisible = ref(false);
const detail = ref<any | null>(null);

async function loadData() {
  loading.value = true;
  try {
    const res = await chatTraceApi.list({
      page: page.value,
      pageSize: pageSize.value,
      userId: filterUserId.value || undefined,
      chatId: filterChatId.value || undefined,
      status: filterStatus.value || undefined,
      minDurationMs: filterMinDuration.value,
      maxDurationMs: filterMaxDuration.value,
    });
    items.value = res.data?.items || [];
    total.value = res.data?.total || 0;
  } catch {
    /* handled by interceptor */
  } finally {
    loading.value = false;
  }
}

async function showDetail(row: any) {
  try {
    const res = await chatTraceApi.detail(row.id);
    detail.value = res.data;
    detailVisible.value = true;
  } catch {
    ElMessage.error("加载详情失败");
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text || "");
    ElMessage.success("已复制");
  } catch {
    ElMessage.error("复制失败");
  }
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
.page-head {
  .filters { display: flex; align-items: center; }
}

:deep(.el-table) {
  .el-table__body tr.el-table__row--striped td {
    background: #F9FAFB !important;
    color: #1F2937 !important;
  }
  .el-table__body tr td {
    color: #1F2937 !important;
  }
  .el-table__body tr:hover > td {
    background: #F9FAFB !important;
  }
}

.detail-body {
  .meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 12px;
    font-size: 13px;
    color: #6B7280;
    padding-bottom: 8px;
    border-bottom: 1px dashed #E5E7EB;
    > div { min-width: 140px; }
  }
  .block {
    margin-top: 16px;
    .block-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }
    .block-body {
      white-space: pre-wrap;
      word-break: break-all;
      background: #F9FAFB;
      padding: 12px;
      border-radius: 8px;
      font-size: 13px;
      max-height: 280px;
      overflow: auto;
      margin: 0;
      font-family: inherit;
      color: #1F2937;
    }
    .block-error {
      background: rgba(239, 68, 68, 0.15);
      color: #F87171;
    }
  }
}
</style>
