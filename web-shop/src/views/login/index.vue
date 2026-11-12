<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/home">← 返回首页</RouterLink>
    <section class="auth-card surface-card">
      <SvgIcon name="logo" />
      <span>FreshMeal 用户端</span>
      <h1>欢迎回来</h1>
      <p>登录后继续管理餐品选择、订单与配送进度。</p>
      <LoginForm :loading="loading" :error="errorMessage" @submit="login" />
      <p class="auth-switch">
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { ShopAuthRequest } from '@/api/modules/auth';
import { useUserStore } from '@/store/modules/user';
import { isApiError } from '@/types/api/common';
import { getSafeRedirect } from '@/utils/url';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);
const errorMessage = ref('');

onScopeDispose(() => userStore.cancelLogin());

const login = async (params: ShopAuthRequest) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await userStore.login(params);
    await router.replace(getSafeRedirect(route.query.redirect, '/member'));
  } catch (error) {
    if (isApiError(error) && error.kind === 'cancelled') return;
    errorMessage.value = isApiError(error)
      ? error.message
      : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100dvh;
  padding: calc(var(--space-5) + env(safe-area-inset-top)) var(--space-5)
    calc(var(--space-5) + env(safe-area-inset-bottom));
  background: var(--color-background);
}

.back-link {
  display: inline-flex;
  align-items: center;
  min-height: var(--touch-target);
  color: var(--color-text-secondary);
  text-decoration: none;
}

.auth-card {
  max-width: 28rem;
  padding: var(--space-6);
  margin: 5vh auto 0;

  > .svg-icon {
    font-size: 2rem;
    color: var(--color-primary);
  }

  > span {
    display: block;
    margin-top: var(--space-5);
    font-size: var(--text-caption);
    font-weight: 750;
    color: var(--color-primary);
  }

  h1 {
    margin: var(--space-2) 0;
    font-size: var(--text-page-title);
    font-weight: 780;
  }

  > p {
    margin: 0 0 var(--space-6);
    color: var(--color-text-secondary);
  }
}

.auth-switch {
  margin-top: var(--space-5) !important;
  margin-bottom: 0 !important;
  text-align: center;

  a {
    font-weight: 700;
    color: var(--color-primary);
  }
}
</style>
