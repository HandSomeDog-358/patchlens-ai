<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api, type PlatformConfig } from '../api/client';

const configs = ref<PlatformConfig[]>([]);
const editingId = ref<number | null>(null);
const form = reactive({
  provider: 'GITEE',
  apiBaseUrl: 'https://gitee.com/api/v5',
  accessToken: '',
  webhookSecret: '',
  enabled: true,
});

function resetForm() {
  editingId.value = null;
  form.provider = 'GITEE';
  form.apiBaseUrl = 'https://gitee.com/api/v5';
  form.accessToken = '';
  form.webhookSecret = '';
  form.enabled = true;
}

async function load() {
  const response = await api.get<PlatformConfig[]>('/platform-configs');
  configs.value = response.data;
}

async function save() {
  const payload = {
    provider: form.provider,
    apiBaseUrl: form.apiBaseUrl,
    accessToken: form.accessToken,
    webhookSecret: form.webhookSecret,
    enabled: form.enabled,
  };
  if (editingId.value) {
    await api.put(`/platform-configs/${editingId.value}`, payload);
  } else {
    await api.post('/platform-configs', payload);
  }
  resetForm();
  await load();
}

function edit(config: PlatformConfig) {
  editingId.value = config.id;
  form.provider = config.provider;
  form.apiBaseUrl = config.apiBaseUrl;
  form.accessToken = '';
  form.webhookSecret = '';
  form.enabled = config.enabled;
}

async function remove(config: PlatformConfig) {
  await api.delete(`/platform-configs/${config.id}`);
  await load();
}

function setProviderDefaults() {
  if (form.provider === 'GITEE') {
    form.apiBaseUrl = 'https://gitee.com/api/v5';
  }
  if (form.provider === 'GITEA') {
    form.apiBaseUrl = 'https://gitea.example.com/api/v1';
  }
  if (form.provider === 'GITHUB') {
    form.apiBaseUrl = 'https://api.github.com';
  }
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>平台配置</h1>
      <p>配置 GitHub、Gitee 和企业内部 Gitea 的 API 地址、访问令牌与 Webhook secret。</p>
    </div>
    <button class="btn secondary" type="button" @click="resetForm">新建</button>
  </section>

  <section class="panel">
    <form class="form-grid" @submit.prevent="save">
      <div class="field">
        <label>平台</label>
        <select v-model="form.provider" class="select" :disabled="editingId !== null" @change="setProviderDefaults">
          <option value="GITEE">Gitee</option>
          <option value="GITEA">Gitea</option>
          <option value="GITHUB">GitHub</option>
        </select>
      </div>
      <div class="field">
        <label>API Base URL</label>
        <input v-model="form.apiBaseUrl" class="input" required />
      </div>
      <div class="field">
        <label>访问令牌</label>
        <input v-model="form.accessToken" class="input" type="password" placeholder="留空表示不更新" />
      </div>
      <div class="field">
        <label>Webhook secret</label>
        <input v-model="form.webhookSecret" class="input" type="password" placeholder="留空表示不更新" />
      </div>
      <div class="toolbar">
        <label class="checkline">
          <input v-model="form.enabled" type="checkbox" />
          启用
        </label>
        <button class="btn" type="submit">{{ editingId ? '更新配置' : '保存配置' }}</button>
      </div>
    </form>
  </section>

  <section class="panel" style="margin-top: 16px">
    <table class="table">
      <thead>
        <tr>
          <th>平台</th>
          <th>API Base URL</th>
          <th>访问令牌</th>
          <th>Webhook secret</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="config in configs" :key="config.id">
          <td>{{ config.provider }}</td>
          <td>{{ config.apiBaseUrl }}</td>
          <td>{{ config.hasAccessToken ? '已设置' : '未设置' }}</td>
          <td>{{ config.hasWebhookSecret ? '已设置' : '未设置' }}</td>
          <td>
            <span class="status" :class="{ completed: config.enabled }">
              {{ config.enabled ? '启用' : '停用' }}
            </span>
          </td>
          <td class="action-cell">
            <div class="table-actions">
              <button class="btn secondary" type="button" @click="edit(config)">编辑</button>
              <button class="btn danger" type="button" @click="remove(config)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
