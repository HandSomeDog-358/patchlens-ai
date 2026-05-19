<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api, type AuthUser } from './api/client';

const route = useRoute();
const router = useRouter();
const currentUser = ref<AuthUser | null>(null);
const isLoginRoute = computed(() => route.path === '/login');

async function loadCurrentUser() {
  if (isLoginRoute.value) {
    currentUser.value = null;
    return;
  }
  try {
    const response = await api.get<AuthUser>('/auth/me');
    currentUser.value = response.data.authenticated ? response.data : null;
  } catch {
    currentUser.value = null;
  }
}

async function logout() {
  await api.post('/auth/logout');
  currentUser.value = null;
  await router.push('/login');
}

watch(
  () => route.path,
  () => {
    void loadCurrentUser();
  },
  { immediate: true },
);
</script>

<template>
  <RouterView v-if="isLoginRoute" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">PL</div>
        <div>
          <strong>PatchLens AI</strong>
          <span>变更透镜</span>
        </div>
      </div>

      <nav class="nav">
        <RouterLink to="/">总览</RouterLink>
        <RouterLink to="/repositories">仓库</RouterLink>
        <RouterLink to="/reviews">审查记录</RouterLink>
        <RouterLink to="/platforms">平台配置</RouterLink>
        <RouterLink to="/platform-capabilities">平台能力</RouterLink>
        <RouterLink to="/quality-control">质量控制</RouterLink>
        <RouterLink to="/models">模型配置</RouterLink>
        <RouterLink to="/accounts">账号管理</RouterLink>
      </nav>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <strong>控制台</strong>
          <span class="muted">代码审查与重构副驾驶</span>
        </div>
        <div v-if="currentUser" class="user-menu">
          <span>{{ currentUser.displayName || currentUser.username }}</span>
          <span class="status ok">{{ currentUser.role }}</span>
          <button class="btn secondary compact" type="button" @click="logout">退出</button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
