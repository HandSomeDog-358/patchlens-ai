<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api, type ReviewFeedback, type ReviewFinding, type ReviewTask } from '../api/client';

const route = useRoute();
const review = ref<ReviewTask | null>(null);
const findings = ref<ReviewFinding[]>([]);
const severityFilter = ref('');
const lastRefreshedAt = ref('');
const feedbackSubmitting = ref<Record<number, string>>({});
let refreshTimer: number | undefined;
let loading = false;

const severityOptions = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const feedbackOptions = [
  { value: 'USEFUL', label: '有用' },
  { value: 'FALSE_POSITIVE', label: '误报' },
  { value: 'FIXED', label: '已修复' },
  { value: 'IGNORED', label: '忽略' },
];

const severityCounts = computed(() => {
  const counts: Record<string, number> = {
    CRITICAL: 0,
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0,
  };
  for (const finding of findings.value) {
    counts[finding.severity] = (counts[finding.severity] || 0) + 1;
  }
  return counts;
});

const filteredFindings = computed(() => {
  if (!severityFilter.value) {
    return findings.value;
  }
  return findings.value.filter((finding) => finding.severity === severityFilter.value);
});

const highestSeverity = computed(() => (
  severityOptions.find((severity) => severityCounts.value[severity] > 0) || '无风险发现'
));

function reviewSubtitle(review: ReviewTask | null) {
  if (!review) {
    return '';
  }
  if (review.targetType === 'COMMIT') {
    return `${review.repositoryName} Commit ${review.commitSha.slice(0, 12)}`;
  }
  return `${review.repositoryName} PR #${review.prNumber}`;
}

function isActive(review: ReviewTask | null) {
  return review?.status === 'QUEUED' || review?.status === 'RUNNING';
}

function formatTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
}

function durationText(review: ReviewTask | null) {
  if (!review) {
    return '-';
  }
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

function feedbackLabel(value: string | null | undefined) {
  return feedbackOptions.find((option) => option.value === value)?.label || value || '';
}

function feedbackCount(finding: ReviewFinding, value: string) {
  return finding.feedbackCounts?.[value] || 0;
}

async function load() {
  if (loading) {
    return;
  }
  loading = true;
  const id = route.params.id;
  try {
    const [reviewResponse, findingsResponse] = await Promise.all([
      api.get<ReviewTask>(`/reviews/${id}`),
      api.get<ReviewFinding[]>(`/reviews/${id}/findings`),
    ]);
    review.value = reviewResponse.data;
    findings.value = findingsResponse.data;
    lastRefreshedAt.value = formatTime(new Date().toISOString());
    updatePolling();
  } finally {
    loading = false;
  }
}

async function rerun() {
  const id = route.params.id;
  await api.post(`/reviews/${id}/rerun`);
  await load();
}

async function cancel() {
  const id = route.params.id;
  await api.post(`/reviews/${id}/cancel`);
  await load();
}

async function submitFeedback(finding: ReviewFinding, value: string) {
  feedbackSubmitting.value = { ...feedbackSubmitting.value, [finding.id]: value };
  try {
    await api.post<ReviewFeedback>(`/findings/${finding.id}/feedback`, { value });
    await load();
  } finally {
    const next = { ...feedbackSubmitting.value };
    delete next[finding.id];
    feedbackSubmitting.value = next;
  }
}

function updatePolling() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
    refreshTimer = undefined;
  }
  if (isActive(review.value)) {
    refreshTimer = window.setInterval(load, 2000);
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
      <h1>Review #{{ review?.id }}</h1>
      <p>{{ reviewSubtitle(review) }}</p>
    </div>
    <div class="toolbar">
      <button
        v-if="review?.status === 'QUEUED' || review?.status === 'RUNNING'"
        class="btn danger"
        type="button"
        @click="cancel"
      >
        取消审查
      </button>
      <button class="btn secondary" type="button" @click="rerun">重新审查</button>
    </div>
  </section>

  <section class="panel" v-if="review">
    <div class="toolbar">
      <span class="status" :class="review.status.toLowerCase()">{{ review.status }}</span>
      <span v-if="review.conclusion" class="status" :class="review.conclusion.toLowerCase()">{{ review.conclusion }}</span>
      <span class="muted">{{ review.commitSha }}</span>
      <span v-if="lastRefreshedAt" class="muted">最后刷新 {{ lastRefreshedAt }}</span>
    </div>
    <p>{{ review.summary || (review.status === 'QUEUED' || review.status === 'RUNNING' ? '审查正在后台执行' : '暂无摘要') }}</p>
    <p v-if="review.status === 'FAILED' && review.errorMessage" class="error-text">
      {{ review.errorMessage }}
    </p>
    <p v-if="review.publishError" class="error-text">
      PR 评论回写失败：{{ review.publishError }}
    </p>
  </section>

  <section class="grid cols-5" style="margin-top: 16px" v-if="review">
    <div class="panel metric mini">
      <span>创建时间</span>
      <strong>{{ formatTime(review.createdAt) }}</strong>
    </div>
    <div class="panel metric mini">
      <span>开始时间</span>
      <strong>{{ formatTime(review.startedAt) }}</strong>
    </div>
    <div class="panel metric mini">
      <span>完成时间</span>
      <strong>{{ formatTime(review.finishedAt) }}</strong>
    </div>
    <div class="panel metric mini">
      <span>审查耗时</span>
      <strong>{{ durationText(review) }}</strong>
    </div>
    <div class="panel metric mini">
      <span>评论回写</span>
      <strong>{{ review.publishedAt ? formatTime(review.publishedAt) : '-' }}</strong>
    </div>
  </section>

  <section class="grid cols-5" style="margin-top: 16px" v-if="review">
    <div class="panel metric">
      <span>最高风险</span>
      <strong class="metric-text">{{ highestSeverity }}</strong>
    </div>
    <button
      class="panel metric metric-button"
      type="button"
      :class="{ active: severityFilter === 'CRITICAL' }"
      @click="severityFilter = severityFilter === 'CRITICAL' ? '' : 'CRITICAL'"
    >
      <span>严重</span>
      <strong>{{ severityCounts.CRITICAL }}</strong>
    </button>
    <button
      class="panel metric metric-button"
      type="button"
      :class="{ active: severityFilter === 'HIGH' }"
      @click="severityFilter = severityFilter === 'HIGH' ? '' : 'HIGH'"
    >
      <span>高</span>
      <strong>{{ severityCounts.HIGH }}</strong>
    </button>
    <button
      class="panel metric metric-button"
      type="button"
      :class="{ active: severityFilter === 'MEDIUM' }"
      @click="severityFilter = severityFilter === 'MEDIUM' ? '' : 'MEDIUM'"
    >
      <span>中</span>
      <strong>{{ severityCounts.MEDIUM }}</strong>
    </button>
    <button
      class="panel metric metric-button"
      type="button"
      :class="{ active: severityFilter === 'LOW' }"
      @click="severityFilter = severityFilter === 'LOW' ? '' : 'LOW'"
    >
      <span>低</span>
      <strong>{{ severityCounts.LOW }}</strong>
    </button>
  </section>

  <section class="panel" style="margin-top: 16px" v-if="findings.length > 0">
    <div class="toolbar">
      <strong>风险发现</strong>
      <button v-if="severityFilter" class="btn secondary compact" type="button" @click="severityFilter = ''">
        清除筛选
      </button>
      <span class="muted">当前显示 {{ filteredFindings.length }} / {{ findings.length }} 条</span>
    </div>
  </section>

  <section class="finding-list" style="margin-top: 16px">
    <article class="finding" v-for="finding in filteredFindings" :key="finding.id">
      <span class="status severity" :class="finding.severity.toLowerCase()">
        {{ finding.severity }} · {{ Math.round(finding.confidence * 100) }}%
      </span>
      <h3>{{ finding.title }}</h3>
      <p class="muted">{{ finding.filePath }}:{{ finding.lineNumber }}</p>
      <p>{{ finding.description }}</p>
      <p><strong>证据：</strong>{{ finding.evidence }}</p>
      <p><strong>建议：</strong>{{ finding.suggestion }}</p>
      <pre v-if="finding.patch" class="code-block"><code>{{ finding.patch }}</code></pre>
      <div class="finding-feedback">
        <div>
          <strong>反馈闭环</strong>
          <span v-if="finding.latestFeedbackValue" class="muted">
            最近标记：{{ feedbackLabel(finding.latestFeedbackValue) }}
          </span>
        </div>
        <div class="table-actions">
          <button
            v-for="option in feedbackOptions"
            :key="option.value"
            class="btn secondary compact"
            type="button"
            :disabled="feedbackSubmitting[finding.id] === option.value"
            @click="submitFeedback(finding, option.value)"
          >
            {{ option.label }} {{ feedbackCount(finding, option.value) || '' }}
          </button>
        </div>
      </div>
    </article>
    <section class="panel empty-state" v-if="review && findings.length === 0 && review.status === 'COMPLETED'">
      <strong>没有发现明显风险</strong>
      <p class="muted">本次审查未返回可展示的风险项。</p>
    </section>
  </section>
</template>
