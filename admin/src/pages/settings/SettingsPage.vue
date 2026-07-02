<template>
  <div class="page-wrap">
    <h3>系统设置</h3>
    <el-divider />

    <!-- 修改密码 -->
    <h4 style="margin-bottom: 16px;">修改密码</h4>
    <el-form :model="pwdForm" label-width="100px" style="max-width: 420px;" :rules="pwdRules" ref="pwdFormRef">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleChangePwd" :loading="pwdLoading">修改密码</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <!-- AI 对话偏好 -->
    <h4 style="margin-bottom: 16px;">AI 对话偏好</h4>
    <el-form label-width="100px" style="max-width: 420px;">
      <el-form-item label="默认模型">
        <el-radio-group v-model="chatPrefs.chat_model" @change="saveChatPrefs">
          <el-radio value="deepseek">DeepSeek</el-radio>
          <el-radio value="dashscope">通义千问</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="对话模式">
        <el-radio-group v-model="chatPrefs.chat_stream" @change="saveChatPrefs">
          <el-radio :value="true">流式</el-radio>
          <el-radio :value="false">同步</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-divider />

    <el-descriptions :column="1" border>
      <el-descriptions-item label="后端服务">Spring Boot 3.3.5</el-descriptions-item>
      <el-descriptions-item label="数据库">PostgreSQL 16</el-descriptions-item>
      <el-descriptions-item label="OCR 引擎">百度 OCR API</el-descriptions-item>
      <el-descriptions-item label="LLM 引擎">DeepSeek / 通义千问</el-descriptions-item>
      <el-descriptions-item label="AI 框架">Spring AI 1.1.7</el-descriptions-item>
      <el-descriptions-item label="向量模型">DashScope text-embedding-v4</el-descriptions-item>
      <el-descriptions-item label="版本">0.2.1</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import http from "@/utils/request";

// ---- AI 对话偏好 ----
const chatPrefs = reactive({ chat_model: "deepseek", chat_stream: true });

async function loadChatPrefs() {
  try {
    const res = await http.get("/checkin/settings/chat");
    if (res.data) {
      chatPrefs.chat_model = res.data.chat_model || "deepseek";
      chatPrefs.chat_stream = res.data.chat_stream !== false;
    }
  } catch {}
}

async function saveChatPrefs() {
  try {
    await http.put("/checkin/settings/chat", {
      chat_model: chatPrefs.chat_model,
      chat_stream: chatPrefs.chat_stream,
    });
    ElMessage.success("偏好已保存");
  } catch { ElMessage.error("保存失败"); }
}

onMounted(loadChatPrefs);

const pwdForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});
const pwdLoading = ref(false);
const pwdFormRef = ref<FormInstance>();

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error("两次密码不一致"));
  } else {
    callback();
  }
};

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "至少6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    { validator: validateConfirm, trigger: "blur" },
  ],
};

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  pwdLoading.value = true;
  try {
    const res = await http.put("/admin/password", {
      old_password: pwdForm.oldPassword,
      new_password: pwdForm.newPassword,
    });
    if (res.data?.ok) {
      ElMessage.success("密码修改成功");
      pwdForm.oldPassword = "";
      pwdForm.newPassword = "";
      pwdForm.confirmPassword = "";
    } else {
      ElMessage.error(res.data?.detail || "修改失败");
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.detail || "修改失败");
  } finally {
    pwdLoading.value = false;
  }
}
</script>

<style lang="scss" scoped>
h4 { font-size: 14px; font-weight: 600; color: #6B7280; }
</style>
