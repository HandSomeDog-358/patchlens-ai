<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import {
  api,
  getApiErrorMessage,
  type ModelConfig,
  type ModelConfigTestResult,
  type ModelOption,
} from '../api/client';

const configs = ref<ModelConfig[]>([]);
const editingId = ref<number | null>(null);
const testingId = ref<number | null>(null);
const modelOptions = ref<ModelOption[]>([]);
const modelListLoading = ref(false);
const modelListMessage = ref('');
const modelListError = ref('');
const testResults = ref<Record<number, ModelConfigTestResult>>({});
const testErrors = ref<Record<number, string>>({});
const form = reactive({
  provider: '',
  baseUrl: '',
  modelName: '',
  apiKey: '',
  enabled: false,
});

async function load() {
  const response = await api.get<ModelConfig[]>('/model-configs');
  configs.value = response.data;
}

function resetForm() {
  editingId.value = null;
  form.provider = '';
  form.baseUrl = '';
  form.modelName = '';
  form.apiKey = '';
  form.enabled = false;
  modelOptions.value = [];
  modelListMessage.value = '';
  modelListError.value = '';
}

async function save() {
  if (editingId.value) {
    await api.put(`/model-configs/${editingId.value}`, form);
  } else {
    await api.post('/model-configs', form);
  }
  resetForm();
  await load();
}

function edit(config: ModelConfig) {
  editingId.value = config.id;
  form.provider = config.provider;
  form.baseUrl = config.baseUrl;
  form.modelName = config.modelName;
  form.apiKey = '';
  form.enabled = config.enabled;
  modelOptions.value = [];
  modelListMessage.value = config.hasApiKey
    ? '编辑已有配置时，可直接使用已保存密钥获取模型列表；如果修改了 Base URL，请重新输入 API Key 或先保存。'
    : '编辑已有配置时，如需获取模型列表，请先输入 API Key。';
  modelListError.value = '';
}

async function fetchModels() {
  modelListMessage.value = '';
  modelListError.value = '';
  modelOptions.value = [];
  if (!form.baseUrl.trim()) {
    modelListError.value = '请先输入 Base URL。';
    return;
  }
  const editingConfig = editingId.value ? configs.value.find((config) => config.id === editingId.value) : null;
  if (!form.apiKey.trim() && !editingId.value) {
    modelListError.value = '请先输入 API Key。';
    return;
  }
  if (!form.apiKey.trim() && editingId.value && !editingConfig?.hasApiKey) {
    modelListError.value = '当前配置还没有保存 API Key，请先输入 API Key。';
    return;
  }
  modelListLoading.value = true;
  try {
    const response = editingId.value && !form.apiKey.trim()
      ? await api.get<ModelOption[]>(`/model-configs/${editingId.value}/models`)
      : await api.post<ModelOption[]>('/model-configs/models', {
          baseUrl: form.baseUrl,
          apiKey: form.apiKey,
        });
    modelOptions.value = response.data;
    modelListMessage.value = response.data.length
      ? `已获取 ${response.data.length} 个模型，请从列表选择。`
      : '网关返回成功，但没有发现可选择的模型。';
  } catch (error) {
    modelListError.value = getApiErrorMessage(error, '获取模型列表失败，请检查 Base URL 或 API Key。');
  } finally {
    modelListLoading.value = false;
  }
}

async function activate(config: ModelConfig) {
  await api.post(`/model-configs/${config.id}/activate`);
  await load();
}

async function test(config: ModelConfig) {
  testingId.value = config.id;
  delete testErrors.value[config.id];
  try {
    const response = await api.post<ModelConfigTestResult>(`/model-configs/${config.id}/test`);
    testResults.value = {
      ...testResults.value,
      [config.id]: response.data,
    };
  } catch (error) {
    testErrors.value = {
      ...testErrors.value,
      [config.id]: getApiErrorMessage(error, '模型连接测试失败'),
    };
  } finally {
    testingId.value = null;
  }
}

async function remove(config: ModelConfig) {
  await api.delete(`/model-configs/${config.id}`);
  await load();
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>模型配置</h1>
      <p>配置 OpenAI compatible 模型网关，后续审查引擎会从这里选择模型。</p>
    </div>
  </section>

  <section class="panel">
    <div class="toolbar">
      <strong>{{ editingId ? '编辑模型网关' : '新增模型网关' }}</strong>
      <button v-if="editingId" class="btn secondary" type="button" @click="resetForm">取消编辑</button>
    </div>
    <form class="form-grid" @submit.prevent="save">
      <div class="field">
        <label>Provider</label>
        <input v-model="form.provider" class="input" required placeholder="例如 openai-compatible" />
      </div>
      <div class="field">
        <label>Base URL</label>
        <input v-model="form.baseUrl" class="input" required placeholder="例如 https://dashscope.aliyuncs.com/compatible-mode/v1" />
      </div>
      <div class="field">
        <label>模型</label>
        <input v-model="form.modelName" class="input" required placeholder="可先获取模型列表后选择" />
      </div>
      <div class="field">
        <label>API Key</label>
        <input v-model="form.apiKey" class="input" type="password" placeholder="保存后不会回显" />
      </div>
      <div class="field">
        <label>模型列表</label>
        <button class="btn secondary" type="button" :disabled="modelListLoading" @click="fetchModels">
          {{ modelListLoading ? '获取中' : '获取模型列表' }}
        </button>
      </div>
      <div class="field" v-if="modelOptions.length > 0">
        <label>选择模型</label>
        <select v-model="form.modelName" class="select">
          <option v-for="model in modelOptions" :key="model.id" :value="model.id">
            {{ model.id }}{{ model.ownedBy ? ` · ${model.ownedBy}` : '' }}
          </option>
        </select>
      </div>
      <div class="field checkbox-field">
        <label>
          <input v-model="form.enabled" type="checkbox" />
          保存后设为当前使用
        </label>
      </div>
      <button class="btn" type="submit">{{ editingId ? '保存修改' : '保存配置' }}</button>
    </form>
    <p v-if="modelListMessage" class="success-text">{{ modelListMessage }}</p>
    <p v-if="modelListError" class="error-text">{{ modelListError }}</p>
  </section>

  <section class="panel" style="margin-top: 16px">
    <table class="table">
      <thead>
        <tr>
          <th>Provider</th>
          <th>Base URL</th>
          <th>模型</th>
          <th>密钥</th>
          <th>当前使用</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="config in configs" :key="config.id">
          <td>{{ config.provider }}</td>
          <td>{{ config.baseUrl }}</td>
          <td>{{ config.modelName }}</td>
          <td>
            <span class="status" :class="{ completed: config.hasApiKey }">
              {{ config.hasApiKey ? '已配置' : '未配置' }}
            </span>
          </td>
          <td>
            <span class="status" :class="{ completed: config.enabled }">
              {{ config.enabled ? '当前' : '候选' }}
            </span>
          </td>
          <td>
            <div class="table-actions">
              <button class="btn secondary" type="button" @click="edit(config)">编辑</button>
              <button class="btn secondary" type="button" :disabled="testingId === config.id" @click="test(config)">
                {{ testingId === config.id ? '测试中' : '测试' }}
              </button>
              <button
                class="btn secondary"
                type="button"
                :disabled="config.enabled"
                @click="activate(config)"
              >
                设为当前
              </button>
              <button class="btn danger" type="button" @click="remove(config)">删除</button>
            </div>
            <p v-if="testResults[config.id]" class="muted">
              {{ testResults[config.id].success ? '测试通过' : '测试失败' }} ·
              {{ testResults[config.id].latencyMs }}ms ·
              {{ testResults[config.id].message }}
            </p>
            <p v-if="testErrors[config.id]" class="error-text">{{ testErrors[config.id] }}</p>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
