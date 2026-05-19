<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, getApiErrorMessage, type AuthUser } from '../api/client';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref('');
const form = reactive({
  username: 'admin',
  password: '',
});

async function login() {
  loading.value = true;
  error.value = '';
  try {
    await api.post<AuthUser>('/auth/login', form);
    await router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/');
  } catch (err) {
    error.value = getApiErrorMessage(err, '登录失败');
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  try {
    const response = await api.get<AuthUser>('/auth/me');
    if (response.data.authenticated) {
      await router.push('/');
    }
  } catch {
    // stay on login
  }
});
</script>

<template>
  <main class="login-shell">
    <section class="login-panel">
      <div class="brand login-brand">
        <div class="brand-mark">PL</div>
        <div>
          <strong>PatchLens AI</strong>
          <span>登录控制台</span>
        </div>
      </div>

      <form class="grid" @submit.prevent="login">
        <div class="field">
          <label>账号</label>
          <input v-model="form.username" class="input" autocomplete="username" required />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" class="input" type="password" autocomplete="current-password" required />
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
        <button class="btn" type="submit" :disabled="loading">{{ loading ? '登录中' : '登录' }}</button>
      </form>
    </section>
  </main>
</template>
