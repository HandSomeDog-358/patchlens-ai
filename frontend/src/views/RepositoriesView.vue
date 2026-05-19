<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  api,
  getApiErrorMessage,
  type Repository,
  type RepositoryBranch,
  type RepositoryCommit,
  type ReviewPreflightCheck,
  type ReviewPreflightResponse,
  type ReviewTask,
} from '../api/client';

const router = useRouter();
const repositories = ref<Repository[]>([]);
const selectedRepository = ref<Repository | null>(null);
const branches = ref<RepositoryBranch[]>([]);
const recentCommits = ref<RepositoryCommit[]>([]);
const branchesLoading = ref(false);
const commitsLoading = ref(false);
const reviewSubmitting = ref(false);
const reviewError = ref('');
const preflightChecks = ref<ReviewPreflightCheck[]>([]);
const selectedBranch = ref('');
const authorFilter = ref('');
const manualCommitSha = ref('');
const selectedCommitSha = ref('');
const listError = ref('');
let suppressBranchWatch = false;
const form = reactive({
  provider: 'GITEE',
  owner: '',
  name: '',
  defaultBranch: 'main',
});

const commitSha = computed(() => manualCommitSha.value.trim() || selectedCommitSha.value);

async function load() {
  listError.value = '';
  try {
    const response = await api.get<Repository[]>('/repositories');
    repositories.value = response.data;
  } catch (error) {
    listError.value = getApiErrorMessage(error, '加载仓库失败');
  }
}

async function createRepository() {
  await api.post('/repositories', form);
  form.owner = '';
  form.name = '';
  form.defaultBranch = 'main';
  await load();
}

async function toggle(repository: Repository) {
  await api.post(`/repositories/${repository.id}/${repository.enabled ? 'disable' : 'enable'}`);
  await load();
}

async function removeRepository(repository: Repository) {
  const confirmed = window.confirm(
    `确认删除仓库 ${repository.owner}/${repository.name}？该仓库的审查记录、风险发现和质量策略会一起删除。`,
  );
  if (!confirmed) {
    return;
  }
  listError.value = '';
  try {
    await api.delete(`/repositories/${repository.id}`);
    await load();
  } catch (error) {
    listError.value = getApiErrorMessage(error, '删除仓库失败');
  }
}

async function openReviewDialog(repository: Repository) {
  suppressBranchWatch = true;
  selectedRepository.value = repository;
  branches.value = [];
  recentCommits.value = [];
  selectedBranch.value = repository.defaultBranch;
  authorFilter.value = '';
  selectedCommitSha.value = '';
  manualCommitSha.value = '';
  reviewError.value = '';
  preflightChecks.value = [];
  await Promise.all([loadBranches(repository), loadCommits(repository, repository.defaultBranch, '')]);
  suppressBranchWatch = false;
}

async function loadBranches(repository: Repository) {
  branchesLoading.value = true;
  try {
    const response = await api.get<RepositoryBranch[]>(`/repositories/${repository.id}/branches`, {
      params: { limit: 50 },
    });
    const branchNames = new Set(response.data.map((branch) => branch.name));
    branches.value = branchNames.has(repository.defaultBranch)
      ? response.data
      : [{ name: repository.defaultBranch, latestCommitSha: '' }, ...response.data];
  } catch (error) {
    branches.value = [{ name: repository.defaultBranch, latestCommitSha: '' }];
    reviewError.value = `${getApiErrorMessage(error, '分支列表拉取失败')} 将使用默认分支或手动输入 commit。`;
  } finally {
    branchesLoading.value = false;
  }
}

async function loadCommits(repository: Repository, branch: string, author: string) {
  commitsLoading.value = true;
  recentCommits.value = [];
  selectedCommitSha.value = '';
  try {
    const response = await api.get<RepositoryCommit[]>(`/repositories/${repository.id}/commits`, {
      params: { branch, author: author.trim() || undefined, limit: 10 },
    });
    recentCommits.value = response.data;
    selectedCommitSha.value = response.data[0]?.sha ?? '';
  } catch (error) {
    reviewError.value = `${getApiErrorMessage(error, '最近提交拉取失败')} 可以切换分支或直接输入 commit 编码发起审查。`;
  } finally {
    commitsLoading.value = false;
  }
}

async function refreshCommits() {
  if (!selectedRepository.value || !selectedBranch.value) {
    reviewError.value = '请先选择分支。';
    return;
  }
  reviewError.value = '';
  preflightChecks.value = [];
  await loadCommits(selectedRepository.value, selectedBranch.value, authorFilter.value);
}

function closeReviewDialog() {
  if (reviewSubmitting.value) {
    return;
  }
  selectedRepository.value = null;
}

async function createReview() {
  if (!selectedRepository.value || !commitSha.value) {
    reviewError.value = '请选择或输入一个 commit 编码。';
    return;
  }
  reviewSubmitting.value = true;
  reviewError.value = '';
  preflightChecks.value = [];
  try {
    const preflightResponse = await api.post<ReviewPreflightResponse>(
      `/repositories/${selectedRepository.value.id}/commit-reviews/preflight`,
      { commitSha: commitSha.value },
    );
    preflightChecks.value = preflightResponse.data.checks;
    if (!preflightResponse.data.ready) {
      reviewError.value = '预检未通过，请先处理失败项。';
      return;
    }
    const response = await api.post<ReviewTask>(`/repositories/${selectedRepository.value.id}/commit-reviews`, {
      commitSha: commitSha.value,
    });
    selectedRepository.value = null;
    await router.push(`/reviews/${response.data.id}`);
  } catch (error) {
    reviewError.value = getApiErrorMessage(error, '审查任务创建失败，请检查平台配置、commit 编码或模型配置。');
  } finally {
    reviewSubmitting.value = false;
  }
}

watch(selectedBranch, async (branch, previousBranch) => {
  if (suppressBranchWatch || !selectedRepository.value || !branch || branch === previousBranch) {
    return;
  }
  reviewError.value = '';
  preflightChecks.value = [];
  await loadCommits(selectedRepository.value, branch, authorFilter.value);
});

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>仓库</h1>
      <p>接入 GitHub、GitLab、Gitee 或 Gitea 仓库，并为每个仓库维护独立审查策略。</p>
    </div>
  </section>

  <section class="panel">
    <form class="form-grid" @submit.prevent="createRepository">
      <div class="field">
        <label>平台</label>
        <select v-model="form.provider" class="select">
          <option value="GITEE">Gitee</option>
          <option value="GITEA">Gitea</option>
          <option value="GITHUB">GitHub</option>
          <option value="GITLAB">GitLab</option>
        </select>
      </div>
      <div class="field">
        <label>Owner</label>
        <input v-model="form.owner" class="input" required placeholder="example-org" />
      </div>
      <div class="field">
        <label>仓库名</label>
        <input v-model="form.name" class="input" required placeholder="example-service" />
      </div>
      <div class="field">
        <label>默认分支</label>
        <input v-model="form.defaultBranch" class="input" required />
      </div>
      <button class="btn" type="submit">添加仓库</button>
    </form>
  </section>

  <section class="panel" style="margin-top: 16px">
    <p v-if="listError" class="error-text">{{ listError }}</p>
    <table class="table">
      <thead>
        <tr>
          <th>平台</th>
          <th>仓库</th>
          <th>默认分支</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="repository in repositories" :key="repository.id">
          <td>{{ repository.provider }}</td>
          <td>{{ repository.owner }}/{{ repository.name }}</td>
          <td>{{ repository.defaultBranch }}</td>
          <td>
            <span class="status" :class="{ completed: repository.enabled }">
              {{ repository.enabled ? '启用' : '停用' }}
            </span>
          </td>
          <td class="action-cell">
            <div class="table-actions">
              <button class="btn secondary" type="button" @click="toggle(repository)">
                {{ repository.enabled ? '停用' : '启用' }}
              </button>
              <button class="btn secondary" type="button" @click="openReviewDialog(repository)">审查</button>
              <button class="btn danger" type="button" @click="removeRepository(repository)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>

  <div v-if="selectedRepository" class="modal-backdrop" @click.self="closeReviewDialog">
    <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="review-dialog-title">
      <div class="modal-header">
        <div>
          <h2 id="review-dialog-title">选择提交审查</h2>
          <p>{{ selectedRepository.owner }}/{{ selectedRepository.name }}</p>
        </div>
        <button class="btn secondary" type="button" @click="closeReviewDialog">关闭</button>
      </div>

      <div class="field">
        <label>分支</label>
        <select v-model="selectedBranch" class="select" :disabled="branchesLoading || branches.length === 0">
          <option value="">
            {{ branchesLoading ? '正在拉取分支...' : '请选择分支' }}
          </option>
          <option v-for="branch in branches" :key="branch.name" :value="branch.name">
            {{ branch.name }}{{ branch.name === selectedRepository.defaultBranch ? ' · 默认' : '' }}
          </option>
        </select>
      </div>

      <div class="field">
        <label>最近 10 次提交</label>
        <select v-model="selectedCommitSha" class="select" :disabled="commitsLoading || recentCommits.length === 0">
          <option value="">
            {{ commitsLoading ? '正在拉取提交...' : '请选择提交' }}
          </option>
          <option v-for="commit in recentCommits" :key="commit.sha" :value="commit.sha">
            {{ commit.shortSha }} · {{ commit.message }}{{ commit.authorName ? ` · ${commit.authorName}` : '' }}
          </option>
        </select>
      </div>

      <div class="field">
        <label>指定用户</label>
        <input
          v-model="authorFilter"
          class="input"
          placeholder="提交人用户名或名称，可留空"
          @keyup.enter="refreshCommits"
        />
      </div>

      <div class="toolbar">
        <button class="btn secondary" type="button" :disabled="commitsLoading || !selectedBranch" @click="refreshCommits">
          {{ commitsLoading ? '查询中' : '查询提交' }}
        </button>
        <span class="muted">可按分支和提交人筛选后再选择 commit。</span>
      </div>

      <div class="field">
        <label>手动输入 Commit 编码</label>
        <input
          v-model="manualCommitSha"
          class="input"
          placeholder="例如 dd11d02e96a0f9e4b8ad0312fca53f3ae792a474"
        />
      </div>

      <p v-if="reviewError" class="error-text">{{ reviewError }}</p>

      <div v-if="preflightChecks.length" class="check-list">
        <div v-for="check in preflightChecks" :key="check.name" class="check-item">
          <span class="status" :class="check.status.toLowerCase()">{{ check.status }}</span>
          <strong>{{ check.name }}</strong>
          <span class="muted">{{ check.message }}</span>
        </div>
      </div>

      <div class="modal-actions">
        <span class="muted">
          {{ commitSha ? `将审查 ${selectedBranch || '指定分支'} / ${commitSha.slice(0, 12)}` : '等待选择提交' }}
        </span>
        <button class="btn" type="button" :disabled="reviewSubmitting || !commitSha" @click="createReview">
          {{ reviewSubmitting ? '审查中...' : '开始审查' }}
        </button>
      </div>
    </section>
  </div>
</template>
