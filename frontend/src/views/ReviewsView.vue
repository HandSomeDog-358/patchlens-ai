<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { api, getApiErrorMessage, type PageResponse, type ReviewGovernance, type ReviewTask } from '../api/client';

const reviews = ref<ReviewTask[]>([]);
const governance = ref<ReviewGovernance | null>(null);
const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const totalPages = ref(0);
const statusFilter = ref('');
const stuckMinutes = ref(60);
const cleanupMessage = ref('');
const error = ref('');
const lastRefreshedAt = ref('');
let refreshTimer: number | undefined;
let loading = false;

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '等待中', value: 'QUEUED' },
  { label: '执行中', value: 'RUNNING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' },
];

function reviewLabel(review: ReviewTask) {
  if (review.targetType === 'COMMIT') {
    return review.commitSha.slice(0, 12);
  }
  return `#${review.prNumber}`;
}

function isActive(review: ReviewTask) {
  return review.status === 'QUEUED' || review.status === 'RUNNING';
}

function formatTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
}

function durationText(review: ReviewTask) {
  const start = review.startedAt || review.createdAt;
  const end = review.finishedAt || (isActive(review) ? new Date().toISOString() : null);
  if (!start || !end) {
    return '-';
  }
  const seconds = Math.max(0, Math.floor((new Date(end).getTime() - new Date(start).getTime()) / 1000));
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const restSeconds = seconds % 60;
  if (minutes < 60) {
    return `${minutes}m ${restSeconds}s`;
  }
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

async function load() {
  if (loading) {
    return;
  }
  loading = true;
  error.value = '';
  try {
    const [reviewsResponse, governanceResponse] = await Promise.all([
      api.get<PageResponse<ReviewTask>>('/reviews', {
        params: { page: page.value, size: size.value, status: statusFilter.value || undefined },
      }),
      api.get<ReviewGovernance>('/reviews/governance', {
        params: { stuckMinutes: stuckMinutes.value },
      }),
    ]);
    reviews.value = reviewsResponse.data.items;
    totalElements.value = reviewsResponse.data.totalElements;
    totalPages.value = reviewsResponse.data.totalPages;
    governance.value = governanceResponse.data;
    lastRefreshedAt.value = formatTime(new Date().toISOString());
    updatePolling();
  } catch (err) {
    error.value = getApiErrorMessage(err, '加载审查记录失败');
  } finally {
    loading = false;
  }
}

async function goToPage(nextPage: number) {
  page.value = Math.max(0, Math.min(nextPage, Math.max(0, totalPages.value - 1)));
  await load();
}

async function applyFilter() {
  page.value = 0;
  await load();
}

function canCancel(review: ReviewTask) {
  return isActive(review);
}

async function cancel(review: ReviewTask) {
  await api.post(`/reviews/${review.id}/cancel`);
  await load();
}

async function rerun(review: ReviewTask) {
  await api.post(`/reviews/${review.id}/rerun`);
  await load();
}

async function cleanupStuck() {
  const response = await api.post<ReviewTask[]>('/reviews/cleanup-stuck', null, {
    params: { minutes: stuckMinutes.value },
  });
  cleanupMessage.value = `已清理 ${response.data.length} 个卡住任务`;
  await load();
}

function updatePolling() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
    refreshTimer = undefined;
  }
  const hasActiveTask = reviews.value.some(isActive) || Boolean(governance.value?.queued || governance.value?.running);
  if (hasActiveTask) {
    refreshTimer = window.setInterval(load, 3000);
  }
}

onMounted(load);
onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
  }
});
</script>

<template>
  <section class="page-header">
    <div>
      <h1>审查记录</h1>
      <p>查看每次 PR 或 Commit 审查任务的摘要、状态和风险发现。</p>
    </div>
    <div class="toolbar">
      <span v-if="lastRefreshedAt" class="muted">最后刷新 {{ lastRefreshedAt }}</span>
      <button class="btn secondary" type="button" @click="cleanupStuck">清理卡住任务</button>
      <button class="btn secondary" type="button" @click="load">刷新</button>
    </div>
  </section>

  <section class="grid cols-6" style="margin-bottom: 16px" v-if="governance">
    <div class="panel metric">
      <span>等待</span>
      <strong>{{ governance.queued }}</strong>
    </div>
    <div class="panel metric">
      <span>执行中</span>
      <strong>{{ governance.running }}</strong>
    </div>
    <div class="panel metric">
      <span>卡住</span>
      <strong>{{ governance.stuck }}</strong>
    </div>
    <div class="panel metric">
      <span>失败</span>
      <strong>{{ governance.failed }}</strong>
    </div>
    <div class="panel metric">
      <span>已完成</span>
      <strong>{{ governance.completed }}</strong>
    </div>
    <div class="panel metric">
      <span>已取消</span>
      <strong>{{ governance.canceled }}</strong>
    </div>
  </section>

  <section class="panel">
    <div class="toolbar">
      <select v-model="statusFilter" class="select compact-select" @change="applyFilter">
        <option v-for="option in statusOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
      <label class="inline-field">
        卡住阈值
        <input v-model.number="stuckMinutes" class="input short-input" type="number" min="1" max="1440" @change="load" />
        分钟
      </label>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="cleanupMessage" class="muted">{{ cleanupMessage }}</p>
    <table class="table">
      <thead>
        <tr>
          <th>仓库</th>
          <th>目标</th>
          <th>Commit</th>
          <th>状态</th>
          <th>结论</th>
          <th>审查时间</th>
          <th>触发</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="review in reviews" :key="review.id">
          <td>{{ review.repositoryName }}</td>
          <td>{{ review.targetType }} {{ reviewLabel(review) }}</td>
          <td>{{ review.commitSha }}</td>
          <td>
            <span class="status" :class="review.status.toLowerCase()">{{ review.status }}</span>
          </td>
          <td>
            <span v-if="review.conclusion" class="status" :class="review.conclusion.toLowerCase()">
              {{ review.conclusion }}
            </span>
            <span v-else class="muted">-</span>
          </td>
          <td>
            <div class="time-stack">
              <span>创建 {{ formatTime(review.createdAt) }}</span>
              <span>开始 {{ formatTime(review.startedAt) }}</span>
              <strong>耗时 {{ durationText(review) }}</strong>
            </div>
          </td>
          <td>{{ review.triggerType }}</td>
          <td class="action-cell">
            <div class="table-actions">
              <RouterLink class="link" :to="`/reviews/${review.id}`">查看详情</RouterLink>
              <button v-if="canCancel(review)" class="btn danger" type="button" @click="cancel(review)">取消</button>
              <button v-else class="btn secondary" type="button" @click="rerun(review)">重试</button>
            </div>
          </td>
        </tr>
        <tr v-if="reviews.length === 0">
          <td colspan="8" class="muted">暂无符合条件的审查任务</td>
        </tr>
      </tbody>
    </table>
    <div class="pager">
      <span class="muted">
        第 {{ totalPages === 0 ? 0 : page + 1 }} / {{ totalPages }} 页，共 {{ totalElements }} 条
      </span>
      <div class="toolbar">
        <button class="btn secondary" type="button" :disabled="page === 0" @click="goToPage(page - 1)">
          上一页
        </button>
        <button
          class="btn secondary"
          type="button"
          :disabled="page + 1 >= totalPages"
          @click="goToPage(page + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
