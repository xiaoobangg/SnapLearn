<template>
  <div class="page-wrap">
    <div class="page-head">
      <h3>音色管理</h3>
      <div>
        <el-button size="small" @click="openCatalog">浏览官方音色库</el-button>
        <el-button size="small" @click="openEnrolled">已复刻列表</el-button>
        <el-button type="primary" size="small" @click="openAdd">新建音色</el-button>
        <el-link style="margin-left:12px;font-size:13px" href="https://bailian.console.aliyun.com/cn-beijing?tab=model#/efm/model_experience_center/voice?currentTab=voiceTts&secondary=clone&primary=cloning" target="_blank" type="warning">官方控制台 → 声音复刻</el-link>
      </div>
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="provider" label="引擎" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.provider === 'dashscope' ? 'primary' : 'info'">{{ row.provider }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="voice_code" label="音色标识" width="140" />
      <el-table-column prop="is_default" label="默认" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.is_default" type="success" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="is_active" label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.is_active ? 'success' : 'danger'" size="small">{{ row.is_active ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center">
        <template #default="{ row }">
          <el-button size="small" link @click="openTest(row)">测试</el-button>
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="!row.is_default" size="small" link type="warning" @click="doSetDefault(row)">设为默认</el-button>
          <el-button size="small" link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑/新建 dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑音色' : '新建音色'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 龙小淳（女声）" />
        </el-form-item>
        <el-form-item label="引擎">
          <el-select v-model="form.provider" style="width:100%">
            <el-option label="dashscope" value="dashscope" />
          </el-select>
        </el-form-item>
        <el-form-item label="音色标识">
          <el-input v-model="form.voice_code" placeholder="如 longxiaochun_v3" />
        </el-form-item>
        <el-form-item label="音频格式">
          <el-select v-model="form.format" style="width:100%">
            <el-option label="mp3" value="mp3" />
            <el-option label="pcm" value="pcm" />
            <el-option label="wav" value="wav" />
            <el-option label="opus" value="opus" />
          </el-select>
        </el-form-item>
        <el-form-item label="采样率">
          <el-select v-model="form.sample_rate" style="width:100%">
            <el-option label="8000 Hz" :value="8000" />
            <el-option label="16000 Hz" :value="16000" />
            <el-option label="22050 Hz (默认)" :value="22050" />
            <el-option label="24000 Hz" :value="24000" />
            <el-option label="44100 Hz" :value="44100" />
            <el-option label="48000 Hz" :value="48000" />
          </el-select>
        </el-form-item>
        <el-form-item label="音量">
          <el-input-number v-model="form.volume" :min="0" :max="100" />
          <span style="margin-left:8px;color:#909399;font-size:12px">默认 50</span>
        </el-form-item>
        <el-form-item label="语速">
          <el-input-number v-model="form.speech_rate" :min="0.5" :max="2.0" :step="0.1" :precision="1" />
          <span style="margin-left:8px;color:#909399;font-size:12px">默认 1.0</span>
        </el-form-item>
        <el-form-item label="音调">
          <el-input-number v-model="form.pitch" :min="0.5" :max="2.0" :step="0.1" :precision="1" />
          <span style="margin-left:8px;color:#909399;font-size:12px">默认 1.0</span>
        </el-form-item>
        <el-form-item label="指令">
          <el-input v-model="form.instruction" placeholder="如 语气亲切自然（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.is_active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="doSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试 dialog -->
    <el-dialog v-model="testVisible" title="音色测试" width="480px">
      <el-form label-width="80px">
        <el-form-item label="音色">
          <span>{{ testVoice?.name }}</span>
        </el-form-item>
        <el-form-item label="测试文本">
          <el-input v-model="testText" type="textarea" :rows="3" placeholder="输入测试文本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" @click="doTest" :loading="testing">合成并播放</el-button>
      </template>
      <div v-if="testAudioUrl" style="margin-top:12px">
        <audio :src="audioBase + testAudioUrl" controls autoplay style="width:100%" />
      </div>
    </el-dialog>

    <!-- 官方音色库弹窗 -->
    <el-dialog v-model="catalogVisible" title="官方音色库（cosyvoice-v3-flash，勾选后导入）" width="900px">
      <el-table :data="catalogList" stripe max-height="400" @selection-change="onCatalogSelect" ref="catalogTable">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="voice_code" label="音色标识" width="180" />
        <el-table-column prop="language" label="语言" width="100" align="center" />
        <el-table-column prop="features" label="特性" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.features && row.features !== '-'" size="small" type="success">{{ row.features }}</el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" width="170" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.model }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="catalogVisible = false">取消</el-button>
        <el-button type="primary" @click="doImport" :disabled="catalogSelected.length === 0">
          导入选中 ({{ catalogSelected.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 已复刻音色列表 -->
    <el-dialog v-model="enrolledVisible" title="云端已复刻音色" width="800px">
      <el-table :data="enrolledItems" stripe max-height="400">
        <el-table-column prop="voice_id" label="音色 ID" min-width="280" show-overflow-tooltip />
        <el-table-column prop="target_model" label="绑定模型" width="160" />
        <el-table-column prop="status" label="状态" width="80" align="center" />
        <el-table-column prop="gmt_create" label="创建时间" width="160" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="doImportEnrolled(row)">导入</el-button>
            <el-button size="small" type="danger" link @click="doDeleteEnrolled(row.voice_id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { voiceApi } from "@/api";

const loading = ref(false);
const items = ref<any[]>([]);
const audioBase = ref("/");

// Catalog
const catalogVisible = ref(false);
const catalogList = ref<any[]>([]);
const catalogSelected = ref<any[]>([]);

function onCatalogSelect(rows: any[]) { catalogSelected.value = rows; }

// Enrolled
const enrolledVisible = ref(false);
const enrolledItems = ref<any[]>([]);
async function openEnrolled() {
  enrolledVisible.value = true;
  try {
    const res = await voiceApi.listEnrolled();
    enrolledItems.value = res.data || [];
  } catch { enrolledItems.value = []; }
}
async function doImportEnrolled(row: any) {
  try {
    await voiceApi.importEnrolled({ voice_id: row.voice_id, target_model: row.target_model, name: row.voice_id.substring(0, 30) });
    ElMessage.success("已导入");
    loadData();
  } catch { ElMessage.error("导入失败"); }
}
async function doDeleteEnrolled(code: string) {
  try { await ElMessageBox.confirm("确定删除云端音色？", "提示", { type: "warning" }); } catch { return; }
  try {
    await voiceApi.deleteEnrolled(code);
    ElMessage.success("已删除");
    openEnrolled();
  } catch { ElMessage.error("删除失败"); }
}

async function openCatalog() {
  catalogVisible.value = true;
  try {
    const res = await voiceApi.catalog();
    catalogList.value = res.data || [];
    catalogSelected.value = [];
  } catch { /* ignore */ }
}

async function doImport() {
  try {
    await voiceApi.importVoices(catalogSelected.value);
    ElMessage.success("已导入 " + catalogSelected.value.length + " 个音色");
    catalogVisible.value = false;
    loadData();
  } catch { ElMessage.error("导入失败"); }
}

// Form
const formVisible = ref(false);
const isEdit = ref(false);
const form = ref<any>({ name: "", provider: "dashscope", voice_code: "", format: "mp3", sample_rate: 22050, volume: 50, speech_rate: 1.0, pitch: 1.0, instruction: "", description: "", is_active: true });
const editId = ref("");

// Test
const testVisible = ref(false);
const testVoice = ref<any>(null);
const testText = ref("Hello, how are you today?");
const testAudioUrl = ref("");
const testing = ref(false);

async function loadData() {
  loading.value = true;
  try {
    const res = await voiceApi.list();
    items.value = res.data || [];
  } catch { /* ignore */ }
  loading.value = false;
}

function openAdd() {
  isEdit.value = false;
  editId.value = "";
  form.value = { name: "", provider: "dashscope", voice_code: "", format: "mp3", sample_rate: 22050, volume: 50, speech_rate: 1.0, pitch: 1.0, instruction: "", description: "", is_active: true };
  formVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  editId.value = row.id;
  form.value = { ...row };
  formVisible.value = true;
}

async function doSave() {
  try {
    if (isEdit.value) {
      await voiceApi.update(editId.value, form.value);
    } else {
      await voiceApi.create(form.value);
    }
    ElMessage.success("已保存");
    formVisible.value = false;
    loadData();
  } catch { ElMessage.error("保存失败"); }
}

async function doDelete(row: any) {
  try { await ElMessageBox.confirm("确定删除？", "提示", { type: "warning" }); } catch { return; }
  try {
    await voiceApi.delete(row.id);
    ElMessage.success("已删除");
    loadData();
  } catch { ElMessage.error("删除失败"); }
}

async function doSetDefault(row: any) {
  try {
    await voiceApi.setDefault(row.id);
    ElMessage.success("已设为默认");
    loadData();
  } catch { ElMessage.error("设置失败"); }
}

function openTest(row: any) {
  testVoice.value = row;
  testText.value = "Hello, how are you today?";
  testAudioUrl.value = "";
  testVisible.value = true;
}

async function doTest() {
  testing.value = true;
  try {
    const res = await voiceApi.test(testVoice.value.id, testText.value);
    const url = res.data?.audio_url;
    if (url) {
      testAudioUrl.value = url;
      ElMessage.success("合成完成");
    } else {
      ElMessage.error(res.data?.detail || "合成失败");
    }
  } catch { ElMessage.error("合成失败"); }
  testing.value = false;
}

onMounted(() => { loadData(); });
</script>

<style lang="scss" scoped>
</style>
