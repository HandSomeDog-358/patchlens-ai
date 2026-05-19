<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, getApiErrorMessage, type PlatformCapabilitySummary } from '../api/client';

const summary = ref<PlatformCapabilitySummary | null>(null);
const error = ref('');

const capabilityLabels: Array<[keyof PlatformCapabilitySummary['capabilities'][number], string]> = [
  ['supportsRecentCommits', '近十次提交'],
  ['supportsCommitReview', 'Commit 审查'],
  ['supportsPullRequestReview', 'PR 审查'],
  ['supportsWebhook', 'Webhook'],
  ['supportsInlineComments', '行级评论'],
  ['supportsSuggestedPatch', '修复建议'],
];

async function load() {
  error.value = '';
  try {
    const response = await api.get<PlatformCapabilitySummary>('/platform-capabilities');
    summary.value = response.data;
  } catch (err) {
    error.value = getApiErrorMessage(err, '加载平台能力失败');
  }
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>平台能力</h1>
      <p>查看各代码托管平台在当前版本中的接入能力、配置缺口和可用状态。</p>
    </div>
    <button class="btn secondary" type="button" @click="load">刷新</button>
  </section>

  <p v-if="error" class="error-text">{{ error }}</p>

  <section class="grid cols-4" style="margin-bottom: 16px" v-if="summary">
    <div class="panel metric">
      <span>接入仓库</span>
      <strong>{{ summary.repositoryCount }}</strong>
    </div>
    <div class="panel metric">
      <span>启用仓库</span>
      <strong>{{ summary.enabledRepositoryCount }}</strong>
    </div>
    <div class="panel metric">
      <span>启用平台配置</span>
      <strong>{{ summary.configuredPlatformCount }}</strong>
    </div>
    <div class="panel metric">
      <span>当前模型</span>
      <strong class="metric-text">{{ summary.activeModelReady ? summary.activeModelName : '未就绪' }}</strong>
    </div>
  </section>

  <section class="capability-list" v-if="summary">
    <article class="panel capability-card" v-for="capability in summary.capabilities" :key="capability.provider">
      <div class="capability-head">
        <div>
          <h2>{{ capability.displayName }}</h2>
          <p class="muted">{{ capability.configurable ? capability.apiBaseUrl || '等待配置 API Base URL' : '后续版本开放配置' }}</p>
        </div>
        <span class="status" :class="capability.status === 'READY' ? 'ok' : 'warn'">
          {{ capability.status === 'READY' ? '可用' : '需处理' }}
        </span>
      </div>

      <div class="capability-grid">
        <span
          v-for="[key, label] in capabilityLabels"
          :key="key"
          class="status"
          :class="capability[key] ? 'ok' : 'canceled'"
        >
          {{ label }}
        </span>
      </div>

      <div class="grid cols-3">
        <div class="metric mini">
          <span>仓库</span>
          <strong>{{ capability.repositoryCount }}</strong>
        </div>
        <div class="metric mini">
          <span>令牌</span>
          <strong>{{ capability.hasAccessToken ? '已设置' : '缺失' }}</strong>
        </div>
        <div class="metric mini">
          <span>Webhook Secret</span>
          <strong>{{ capability.hasWebhookSecret ? '已设置' : '缺失' }}</strong>
        </div>
      </div>

      <ul v-if="capability.gaps.length" class="plain-list">
        <li v-for="gap in capability.gaps" :key="gap">{{ gap }}</li>
      </ul>
    </article>
  </section>
</template>
