<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>声音复刻</h3>
      <el-button size="small" @click="$router.push('/voices')">← 返回音色管理</el-button>
    </div>

    <el-card style="max-width:600px">
      <el-tabs v-model="tab" type="border-card">
        <!-- 文件上传模式 -->
        <el-tab-pane label="文件上传" name="file">
          <el-form label-width="80px">
            <el-form-item label="音色名称">
              <el-input v-model="form.voiceName" placeholder="如 我的声音" />
            </el-form-item>
            <el-form-item label="绑定模型">
              <el-select v-model="form.targetModel" style="width:100%">
                <el-option label="cosyvoice-v3-flash（推荐·快速）" value="cosyvoice-v3-flash" />
                <el-option label="cosyvoice-v3-plus（高质量）" value="cosyvoice-v3-plus" />
              </el-select>
            </el-form-item>
            <el-form-item label="音频文件">
              <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange" :on-remove="() => file = null" accept="audio/*">
                <el-button size="small" type="primary">选择文件</el-button>
                <template #tip><span class="tip">10~20s，MP3/WAV，≤10MB</span></template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!file || !form.voiceName || enrolling" :loading="enrolling" @click="doEnrollFile">
                {{ enrolling ? '提交中...' : '开始复刻' }}
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="!baseUrlSet" title="本地开发提示" type="info" :closable="false"
            description="未配置 APP_BASE_URL，文件上传模式需要服务器有公网域名。本地开发请用「URL 模式」标签页。" />
        </el-tab-pane>

        <!-- URL 模式 -->
        <el-tab-pane label="URL 模式" name="url">
          <el-form label-width="80px">
            <el-form-item label="音色名称">
              <el-input v-model="form.voiceName" placeholder="如 我的声音" />
            </el-form-item>
            <el-form-item label="绑定模型">
              <el-select v-model="form.targetModel" style="width:100%">
                <el-option label="cosyvoice-v3-flash（推荐·快速）" value="cosyvoice-v3-flash" />
                <el-option label="cosyvoice-v3-plus（高质量）" value="cosyvoice-v3-plus" />
              </el-select>
            </el-form-item>
            <el-form-item label="音频 URL">
              <el-input v-model="form.audioUrl" placeholder="https://example.com/sample.mp3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="!form.audioUrl || !form.voiceName || enrolling" :loading="enrolling" @click="doEnrollUrl">
                {{ enrolling ? '提交中...' : '开始复刻' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div v-if="result" class="result-box">
        <el-alert :title="resultText" :type="resultOk ? 'success' : 'error'" :closable="false" show-icon />
        <el-button v-if="resultOk" type="success" size="small" style="margin-top:12px" @click="$router.push('/voices')">
          进入音色管理
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import http from "@/utils/request";

const tab = ref("file");
const baseUrlSet = ref(false); // 是否检测到 APP_BASE_URL
const form = ref({ voiceName: "", targetModel: "cosyvoice-v3-flash", audioUrl: "" });
const file = ref<any>(null);
const enrolling = ref(false);
const result = ref<any>(null);
const resultOk = ref(false);
const resultText = ref("");

function onFileChange(f: any) { file.value = f.raw; }

async function doEnrollFile() {
  enrolling.value = true;
  result.value = null;
  try {
    const fd = new FormData();
    fd.append("file", file.value);
    fd.append("name", form.value.voiceName);
    fd.append("model", form.value.targetModel);
    const res = await http.post("/admin/voices/enroll", fd, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    handleResult(res.data);
  } catch (e: any) {
    resultOk.value = false;
    resultText.value = "请求失败: " + (e?.message || "");
  }
  enrolling.value = false;
}

async function doEnrollUrl() {
  enrolling.value = true;
  result.value = null;
  try {
    const res = await http.post("/admin/voices/enroll", {
      url: form.value.audioUrl,
      name: form.value.voiceName,
      model: form.value.targetModel,
    });
    handleResult(res.data);
  } catch (e: any) {
    resultOk.value = false;
    resultText.value = "请求失败: " + (e?.message || "");
  }
  enrolling.value = false;
}

function handleResult(data: any) {
  if (data?.ok) {
    resultOk.value = true;
    resultText.value = "复刻成功！" + (data.voice_code || "");
    ElMessage.success("复刻完成");
  } else {
    resultOk.value = false;
    resultText.value = "复刻失败：" + (data?.detail || "");
  }
  result.value = data;
}
</script>

<style lang="scss" scoped>
.page-head { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  margin-bottom: 20px; 
  h3 { 
    font-size: 18px; 
    font-weight: 600;
    color: #F8FAFC;
    display: flex;
    align-items: center;
    gap: 12px;

    &::before {
      content: "";
      width: 4px;
      height: 20px;
      background: linear-gradient(180deg, #4F46E5, #06B6D4);
      border-radius: 2px;
    }
  } 
}
.tip { font-size: 12px; color: #94A3B8; }
.result-box { margin-top: 20px; }
</style>
