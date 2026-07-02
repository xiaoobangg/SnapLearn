<template>
  <div class="log-page">
    <div class="page-header">
      <h2>API 访问日志</h2>
      <div style="display:flex;gap:8px;align-items:center;">
        <el-input v-model="searchUri" placeholder="搜索 URI..." style="width: 300px;" clearable @clear="loadLogs" @keyup.enter="loadLogs" />
        <el-button type="primary" @click="loadLogs">查询</el-button>
      </div>
    </div>

    <el-table :data="items" stripe v-loading="loading" empty-text="暂无记录">
      <el-table-column prop="method" label="方法" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="methodColor(row.method)" size="small">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uri" label="URI" min-width="240" show-overflow-tooltip />
      <el-table-column prop="user_id" label="用户" width="160" show-overflow-tooltip />
      <el-table-column prop="duration_ms" label="耗时(ms)" width="90" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.duration_ms > 1000 ? '#FF6B6B' : '#868E96' }">{{ row.duration_ms }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="时间" width="180" />
      <el-table-column label="详情" width="70" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showDetail(row)">明细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 报文明细弹窗 -->
    <el-dialog v-model="detailVisible" title="报文详情" width="700px" top="5vh">
      <div style="margin-bottom: 12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
          <strong>请求报文</strong>
          <el-button size="small" text @click="copyText(detailReq)">复制</el-button>
        </div>
        <pre style="white-space:pre-wrap;word-break:break-all;background:#f8f9fa;padding:12px;border-radius:6px;font-size:13px;max-height:200px;overflow:auto;">{{ detailReq || '(空)' }}</pre>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
          <strong>返回报文</strong>
          <el-button size="small" text @click="copyText(detailResp)">复制</el-button>
        </div>
        <pre style="white-space:pre-wrap;word-break:break-all;background:#f8f9fa;padding:12px;border-radius:6px;font-size:13px;max-height:200px;overflow:auto;">{{ detailResp || '(空)' }}</pre>
      </div>
    </el-dialog>

    <el-pagination
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadLogs"
      style="margin-top: 16px; justify-content: flex-end;"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import http from "@/utils/request";

const items = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const searchUri = ref("");
const detailVisible = ref(false);
const detailReq = ref("");
const detailResp = ref("");

async function loadLogs() {
  loading.value = true;
  try {
    const res = await http.get("/admin/logs", { params: { page: page.value, pageSize, uri: searchUri.value || undefined } });
    items.value = res.data?.items || [];
    total.value = res.data?.total || 0;
  } catch {
    /* ignore */
  } finally {
    loading.value = false;
  }
}

function showDetail(row: any) {
  detailReq.value = row.request_body || "";
  detailResp.value = row.response_body || "";
  detailVisible.value = true;
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("已复制");
  } catch {
    ElMessage.error("复制失败");
  }
}

function methodColor(method: string) {
  const map: Record<string, string> = { GET: "success", POST: "primary", PUT: "warning", DELETE: "danger" };
  return map[method] || "info";
}

onMounted(loadLogs);
</script>

<style scoped>
.log-page {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #E5E7EB;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
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
</style>
