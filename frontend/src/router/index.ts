import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import RepositoriesView from '../views/RepositoriesView.vue';
import ReviewsView from '../views/ReviewsView.vue';
import ReviewDetailView from '../views/ReviewDetailView.vue';
import ModelConfigView from '../views/ModelConfigView.vue';
import PlatformConfigView from '../views/PlatformConfigView.vue';
import PlatformCapabilitiesView from '../views/PlatformCapabilitiesView.vue';
import QualityControlView from '../views/QualityControlView.vue';
import LoginView from '../views/LoginView.vue';
import AccountsView from '../views/AccountsView.vue';
import { api, type AuthUser } from '../api/client';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/repositories', name: 'repositories', component: RepositoriesView },
    { path: '/reviews', name: 'reviews', component: ReviewsView },
    { path: '/reviews/:id', name: 'review-detail', component: ReviewDetailView },
    { path: '/platforms', name: 'platforms', component: PlatformConfigView },
    { path: '/platform-capabilities', name: 'platform-capabilities', component: PlatformCapabilitiesView },
    { path: '/quality-control', name: 'quality-control', component: QualityControlView },
    { path: '/models', name: 'models', component: ModelConfigView },
    { path: '/accounts', name: 'accounts', component: AccountsView },
  ],
});

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true;
  }
  try {
    const response = await api.get<AuthUser>('/auth/me');
    if (response.data.authenticated) {
      return true;
    }
  } catch {
    // redirect below
  }
  return { path: '/login', query: { redirect: to.fullPath } };
});

export default router;
