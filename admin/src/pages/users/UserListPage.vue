<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>用户管理</h3>
      <el-input v-model="keyword" placeholder="搜索手机号/昵称/openid" clearable style="width:300px" @input="loadData" />
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column label="登录方式" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.wechat_openid ? 'success' : 'info'">
            {{ row.wechat_openid ? '微信' : '手机号' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="150">
        <template #default="{ row }">
          <span :style="{ color: row.phone ? '' : '#ccc' }">{{ row.phone || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" min-width="140" />
      <el-table-column prop="wechat_openid" label="OpenID" width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.wechat_openid" style="font-size:12px;color:#909399">{{ row.wechat_openid }}</span>
          <span v-else style="color:#ccc">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="created_at" label="注册时间" width="180" />
      <el-table-column label="操作" width="300" align="center">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="router.push(`/users/${row.id}`)">详情</el-button>
          <el-button size="small" link type="warning" @click.stop="openRoles(row)">角色</el-button>
          <el-button size="small" link @click.stop="openSettings(row)">AI偏好</el-button>
          <el-button size="small" link type="danger" @click.stop="openResetPwd(row)">重置密码</el-button>
          <el-button size="small" link type="danger" @click.stop="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 角色分配 -->
    <el-dialog v-model="roleVisible" title="角色分配" width="360px">
      <el-form>
        <el-form-item label="用户">{{ roleUserLabel }}</el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="roleList">
            <el-checkbox label="admin">管理员</el-checkbox>
            <el-checkbox label="user">普通用户</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="doSaveRoles">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="360px">
      <el-form>
        <el-form-item label="用户">{{ resetUserLabel }}</el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwdVal" placeholder="至少6位" />
          <span style="color:#909399;font-size:12px">重置为 123456</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doResetPwd">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- AI 对话偏好 -->
    <el-dialog v-model="settingsVisible" title="AI 对话偏好" width="420px">
      <el-form v-if="settingsUser" label-width="80px">
        <el-form-item label="用户">{{ settingsUserLabel }}</el-form-item>
        <el-form-item label="对话模式">
          <el-select v-model="chatPrefs.chat_mode" style="width:100%">
            <el-option label="普通聊天" value="chat" />
            <el-option label="Agent 模式" value="agent" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="chatPrefs.chat_model" style="width:100%">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="通义千问" value="dashscope" />
          </el-select>
        </el-form-item>
        <el-form-item label="流式输出">
          <el-switch v-model="chatPrefs.chat_stream" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="doSaveSettings">保存</el-button>
      </template>
    </el-dialog>

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
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/utils/request";
import { userApi } from "@/api";

const router = useRouter();

function userLabel(row: any) {
  return row.nickname && row.nickname !== "微信用户"
    ? `${row.nickname} (${row.phone || "微信登录"})`
    : (row.phone || "微信用户");
}

// === 角色 ===
const roleVisible = ref(false);
const roleUserLabel = ref("");
const roleUserId = ref("");
const roleList = ref<string[]>([]);

async function openRoles(row: any) {
  roleUserId.value = row.id;
  roleUserLabel.value = userLabel(row);
  roleVisible.value = true;
  try {
    const res = await http.get(`/admin/users/${row.id}/roles`);
    roleList.value = res.data || [];
  } catch { roleList.value = []; }
}
async function doSaveRoles() {
  try {
    await http.put(`/admin/users/${roleUserId.value}/roles`, { roles: roleList.value });
    ElMessage.success("角色已更新");
    roleVisible.value = false;
  } catch { ElMessage.error("保存失败"); }
}

// === 重置密码 ===
const resetVisible = ref(false);
const resetUserLabel = ref("");
const resetUserId = ref("");
const resetPwdVal = ref("123456");

function openResetPwd(row: any) {
  resetUserId.value = row.id;
  resetUserLabel.value = userLabel(row);
  resetPwdVal.value = "123456";
  resetVisible.value = true;
}
async function doResetPwd() {
  try {
    await http.put(`/admin/users/${resetUserId.value}/password`, { new_password: resetPwdVal.value });
    ElMessage.success("密码已重置为 " + resetPwdVal.value);
    resetVisible.value = false;
  } catch { ElMessage.error("重置失败"); }
}
async function doDelete(row: any) {
  try { await ElMessageBox.confirm(`确定删除用户"${row.phone || row.id}"吗？`, "提示", { type: "warning" }); } catch { return; }
  try {
    await userApi.delete(row.id);
    ElMessage.success("已删除");
    loadData();
  } catch { ElMessage.error("删除失败"); }
}

// === AI 偏好 ===
const settingsVisible = ref(false);
const settingsUser = ref<any>(null);
const settingsUserLabel = computed(() => userLabel(settingsUser.value));
const chatPrefs = ref({ chat_mode: "chat", chat_model: "deepseek", chat_stream: true });

async function openSettings(row: any) {
  settingsUser.value = row;
  settingsVisible.value = true;
  try {
    const res = await http.get(`/admin/users/${row.id}/settings`);
    if (res.data) {
      chatPrefs.value.chat_mode = res.data.chat_mode || "chat";
      chatPrefs.value.chat_model = res.data.chat_model || "deepseek";
      chatPrefs.value.chat_stream = res.data.chat_stream !== false;
    }
  } catch { /* use defaults */ }
}
async function doSaveSettings() {
  try {
    await http.put(`/admin/users/${settingsUser.value.id}/settings`, chatPrefs.value);
    ElMessage.success("AI 偏好已保存");
    settingsVisible.value = false;
  } catch { ElMessage.error("保存失败"); }
}

// === 列表 ===
const loading = ref(false);
const items = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const keyword = ref("");

async function loadData() {
  loading.value = true;
  try {
    const res = await userApi.list({ page: page.value, pageSize: pageSize.value, keyword: keyword.value });
    items.value = res.data.items || [];
    total.value = res.data.total || 0;
  } catch { /* handled */ }
  loading.value = false;
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
</style>
