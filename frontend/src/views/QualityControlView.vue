<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { api, getApiErrorMessage, type Repository, type ReviewPolicy } from '../api/client';

const repositories = ref<Repository[]>([]);
const selectedRepositoryId = ref<number | null>(null);
const policy = ref<ReviewPolicy | null>(null);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const message = ref('');

const form = reactive({
  language: 'zh-CN',
  minConfidence: 0.75,
  maxInlineComments: 5,
  enableSummary: true,
  enableInlineComments: true,
  enableSuggestedPatch: true,
  ignoredPaths: '',
  focusPaths: '',
});

const selectedRepository = computed(() => repositories.value.find((item) => item.id === selectedRepositoryId.value));
const strictness = computed(() => {
  if (form.minConfidence >= 0.85 && form.maxInlineComments <= 5) {
    return '严格';
  }
  if (form.minConfidence >= 0.7) {
    return '均衡';
  }
  return '宽松';
});

async function loadRepositories() {
  error.value = '';
  try {
    const response = await api.get<Repository[]>('/repositories');
    repositories.value = response.data;
    if (!selectedRepositoryId.value && response.data.length) {
      selectedRepositoryId.value = response.data[0].id;
    }
  } catch (err) {
    error.value = getApiErrorMessage(err, '加载仓库失败');
  }
}

async function loadPolicy() {
  if (!selectedRepositoryId.value) {
    policy.value = null;
    return;
  }
  loading.value = true;
  error.value = '';
  message.value = '';
  try {
    const response = await api.get<ReviewPolicy>(`/repositories/${selectedRepositoryId.value}/policy`);
    policy.value = response.data;
    form.language = response.data.language;
    form.minConfidence = response.data.minConfidence;
    form.maxInlineComments = response.data.maxInlineComments;
    form.enableSummary = response.data.enableSummary;
    form.enableInlineComments = response.data.enableInlineComments;
    form.enableSuggestedPatch = response.data.enableSuggestedPatch;
    form.ignoredPaths = response.data.ignoredPaths || '';
    form.focusPaths = response.data.focusPaths || '';
  } catch (err) {
    error.value = getApiErrorMessage(err, '加载质量策略失败');
  } finally {
    loading.value = false;
  }
}

async function savePolicy() {
  if (!selectedRepositoryId.value) {
    return;
  }
  saving.value = true;
  error.value = '';
  message.value = '';
  try {
    const response = await api.put<ReviewPolicy>(`/repositories/${selectedRepositoryId.value}/policy`, form);
    policy.value = response.data;
    message.value = '质量控制策略已保存';
  } catch (err) {
    error.value = getApiErrorMessage(err, '保存质量策略失败');
  } finally {
    saving.value = false;
  }
}

function useStrictPreset() {
  form.minConfidence = 0.85;
  form.maxInlineComments = 5;
  form.enableSummary = true;
  form.enableInlineComments = true;
  form.enableSuggestedPatch = true;
}

function useBalancedPreset() {
  form.minConfidence = 0.75;
  form.maxInlineComments = 8;
  form.enableSummary = true;
  form.enableInlineComments = true;
  form.enableSuggestedPatch = true;
}

watch(selectedRepositoryId, loadPolicy);
onMounted(loadRepositories);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>质量控制</h1>
      <p>为每个仓库配置审查阈值、输出粒度、建议补丁和路径范围。</p>
    </div>
    <div class="toolbar">
      <button class="btn secondary" type="button" @click="useBalancedPreset">均衡预设</button>
      <button class="btn secondary" type="button" @click="useStrictPreset">严格预设</button>
    </div>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="message" class="success-text">{{ message }}</p>

  <section class="panel">
    <div class="form-grid">
      <div class="field">
        <label>仓库</label>
        <select v-model="selectedRepositoryId" class="select" :disabled="repositories.length === 0">
          <option v-for="repository in repositories" :key="repository.id" :value="repository.id">
            {{ repository.owner }}/{{ repository.name }}
          </option>
        </select>
      </div>
      <div class="metric mini">
        <span>当前强度</span>
        <strong>{{ strictness }}</strong>
      </div>
      <div class="metric mini">
        <span>平台</span>
        <strong>{{ selectedRepository?.provider || '-' }}</strong>
      </div>
      <div class="metric mini">
        <span>状态</span>
        <strong>{{ selectedRepository?.enabled ? '启用' : '停用' }}</strong>
      </div>
    </div>
  </section>

  <section class="panel" style="margin-top: 16px" v-if="selectedRepositoryId">
    <div class="toolbar">
      <strong>审查质量门槛</strong>
      <span class="muted">{{ loading ? '加载中' : `策略 #${policy?.id || '-'}` }}</span>
    </div>
    <form class="grid" @submit.prevent="savePolicy">
      <div class="form-grid">
        <div class="field">
          <label>反馈语言</label>
          <select v-model="form.language" class="select">
            <option value="zh-CN">中文</option>
            <option value="en-US">English</option>
          </select>
        </div>
        <div class="field">
          <label>最低置信度</label>
          <input v-model.number="form.minConfidence" class="input" type="number" min="0" max="1" step="0.01" />
        </div>
        <div class="field">
          <label>最多行级评论</label>
          <input v-model.number="form.maxInlineComments" class="input" type="number" min="0" max="20" />
        </div>
      </div>

      <div class="toolbar">
        <label class="checkline">
          <input v-model="form.enableSummary" type="checkbox" />
          生成审查摘要
        </label>
        <label class="checkline">
          <input v-model="form.enableInlineComments" type="checkbox" />
          生成行级发现
        </label>
        <label class="checkline">
          <input v-model="form.enableSuggestedPatch" type="checkbox" />
          生成修复建议
        </label>
      </div>

      <div class="form-grid two-cols">
        <div class="field">
          <label>忽略路径</label>
          <textarea
            v-model="form.ignoredPaths"
            class="input textarea"
            placeholder="每行一个 glob，例如 dist/** 或 *.lock"
          />
        </div>
        <div class="field">
          <label>重点路径</label>
          <textarea
            v-model="form.focusPaths"
            class="input textarea"
            placeholder="每行一个 glob，例如 src/payment/**"
          />
        </div>
      </div>

      <div class="panel quality-note">
        <strong>当前质量门禁</strong>
        <p>
          仅展示置信度不低于 {{ Math.round(form.minConfidence * 100) }}% 的发现，
          最多输出 {{ form.maxInlineComments }} 条行级评论；
          {{ form.enableSuggestedPatch ? '会要求模型给出可执行修复建议。' : '不会要求模型生成修复建议。' }}
        </p>
      </div>

      <div class="toolbar">
        <button class="btn" type="submit" :disabled="saving || loading">
          {{ saving ? '保存中' : '保存策略' }}
        </button>
      </div>
    </form>
  </section>
</template>
