<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api, getApiErrorMessage, type AuthUser, type UserAccount } from '../api/client';

const accounts = ref<UserAccount[]>([]);
const currentUser = ref<AuthUser | null>(null);
const editingId = ref<number | null>(null);
const error = ref('');
const loading = ref(false);
const passwordMessage = ref('');
const passwordError = ref('');
const form = reactive({
  username: '',
  displayName: '',
  password: '',
  enabled: true,
});
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

function resetForm() {
  editingId.value = null;
  form.username = '';
  form.displayName = '';
  form.password = '';
  form.enabled = true;
  error.value = '';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [meResponse, accountsResponse] = await Promise.all([
      api.get<AuthUser>('/auth/me'),
      api.get<UserAccount[]>('/accounts'),
    ]);
    currentUser.value = meResponse.data.authenticated ? meResponse.data : null;
    accounts.value = accountsResponse.data;
  } catch (err) {
    error.value = getApiErrorMessage(err, '加载账号失败');
  } finally {
    loading.value = false;
  }
}

function edit(account: UserAccount) {
  editingId.value = account.id;
  form.username = account.username;
  form.displayName = account.displayName;
  form.password = '';
  form.enabled = account.enabled;
  error.value = '';
}

async function save() {
  error.value = '';
  try {
    if (editingId.value) {
      await api.put(`/accounts/${editingId.value}`, {
        displayName: form.displayName,
        password: form.password,
        enabled: form.enabled,
      });
    } else {
      await api.post('/accounts', form);
    }
    resetForm();
    await load();
  } catch (err) {
    error.value = getApiErrorMessage(err, '保存账号失败');
  }
}

async function remove(account: UserAccount) {
  if (account.username === currentUser.value?.username) {
    error.value = '不能删除当前登录账号';
    return;
  }
  const confirmed = window.confirm(`确认删除账号 ${account.username}？`);
  if (!confirmed) {
    return;
  }
  error.value = '';
  try {
    await api.delete(`/accounts/${account.id}`);
    await load();
  } catch (err) {
    error.value = getApiErrorMessage(err, '删除账号失败');
  }
}

async function changePassword() {
  passwordMessage.value = '';
  passwordError.value = '';
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致';
    return;
  }
  try {
    await api.post('/auth/password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    });
    passwordForm.currentPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordMessage.value = '密码已更新，下次登录请使用新密码';
  } catch (err) {
    passwordError.value = getApiErrorMessage(err, '修改密码失败');
  }
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <div>
      <h1>账号管理</h1>
      <p>维护可以登录 PatchLens AI 控制台的管理员账号。</p>
    </div>
    <button class="btn secondary" type="button" @click="resetForm">新建</button>
  </section>

  <section class="panel">
    <div class="toolbar">
      <strong>{{ editingId ? '编辑账号' : '新增账号' }}</strong>
      <span class="muted">密码不会回显，编辑时留空表示不修改密码。</span>
    </div>
    <form class="form-grid" @submit.prevent="save">
      <div class="field">
        <label>账号</label>
        <input v-model="form.username" class="input" required :disabled="editingId !== null" />
      </div>
      <div class="field">
        <label>显示名称</label>
        <input v-model="form.displayName" class="input" required />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" class="input" type="password" :required="editingId === null" />
      </div>
      <label class="checkline">
        <input v-model="form.enabled" type="checkbox" :disabled="form.username === currentUser?.username" />
        启用
      </label>
      <button class="btn" type="submit">{{ editingId ? '保存修改' : '创建账号' }}</button>
    </form>
    <p v-if="error" class="error-text">{{ error }}</p>
  </section>

  <section class="panel" style="margin-top: 16px">
    <div class="toolbar">
      <strong>修改当前密码</strong>
      <span class="muted">至少 8 位，修改后当前会话仍可继续使用。</span>
    </div>
    <form class="form-grid" @submit.prevent="changePassword">
      <div class="field">
        <label>当前密码</label>
        <input v-model="passwordForm.currentPassword" class="input" type="password" autocomplete="current-password" required />
      </div>
      <div class="field">
        <label>新密码</label>
        <input v-model="passwordForm.newPassword" class="input" type="password" autocomplete="new-password" minlength="8" required />
      </div>
      <div class="field">
        <label>确认新密码</label>
        <input v-model="passwordForm.confirmPassword" class="input" type="password" autocomplete="new-password" minlength="8" required />
      </div>
      <button class="btn" type="submit">修改密码</button>
    </form>
    <p v-if="passwordMessage" class="success-text">{{ passwordMessage }}</p>
    <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
  </section>

  <section class="panel" style="margin-top: 16px">
    <div class="toolbar">
      <strong>账号列表</strong>
      <span class="muted">{{ loading ? '加载中' : `共 ${accounts.length} 个账号` }}</span>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>账号</th>
          <th>显示名称</th>
          <th>角色</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="account in accounts" :key="account.id">
          <td>{{ account.username }}</td>
          <td>{{ account.displayName }}</td>
          <td>{{ account.role }}</td>
          <td>
            <span class="status" :class="{ completed: account.enabled }">
              {{ account.enabled ? '启用' : '停用' }}
            </span>
          </td>
          <td class="action-cell">
            <div class="table-actions">
              <button class="btn secondary" type="button" @click="edit(account)">编辑</button>
              <button
                class="btn danger"
                type="button"
                :disabled="account.username === currentUser?.username"
                @click="remove(account)"
              >
                删除
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
