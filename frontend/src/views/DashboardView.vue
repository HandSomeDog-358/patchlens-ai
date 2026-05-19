<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, type PageResponse, type Repository, type ReviewTask } from '../api/client';

const repositories = ref<Repository[]>([]);
const reviews = ref<ReviewTask[]>([]);
const loading = ref(false);

function reviewLabel(review: ReviewTask) {
  if (review.targetType === 'COMMIT') {
    return review.commitSha.slice(0, 8);
  }
  return `#${review.prNumber}`;
}

async function load() {
  loading.value = true;
  try {
    const [repositoryResponse, reviewResponse] = await Promise.all([
      api.get<Repository[]>('/repositories'),
      api.get<PageResponse<ReviewTask>>('/reviews', { params: { page: 0, size: 50 } }),
    ]);
    repositories.value = repositoryResponse.data;
    reviews.value = reviewResponse.data.items;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>审查总览</h1>
      <p>跟踪仓库接入、PR 审查状态和高风险发现。</p>
    </div>
    <button class="btn secondary" type="button" @click="load">刷新</button>
  </section>

  <section class="grid cols-3">
    <div class="panel metric">
      <span>接入仓库</span>
      <strong>{{ repositories.length }}</strong>
    </div>
    <div class="panel metric">
      <span>审查任务</span>
      <strong>{{ reviews.length }}</strong>
    </div>
    <div class="panel metric">
      <span>已完成</span>
      <strong>{{ reviews.filter((review) => review.status === 'COMPLETED').length }}</strong>
    </div>
  </section>

  <section class="panel" style="margin-top: 16px">
    <div class="toolbar">
      <strong>最近审查</strong>
      <span v-if="loading" class="muted">加载中</span>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>仓库</th>
          <th>PR</th>
          <th>状态</th>
          <th>摘要</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="review in reviews.slice(0, 8)" :key="review.id">
          <td>{{ review.repositoryName }}</td>
          <td>
            <RouterLink class="link" :to="`/reviews/${review.id}`">{{ reviewLabel(review) }}</RouterLink>
          </td>
          <td>
            <span class="status" :class="review.status.toLowerCase()">{{ review.status }}</span>
          </td>
          <td>{{ review.summary || '等待审查结果' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
