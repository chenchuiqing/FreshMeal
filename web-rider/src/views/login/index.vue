<template>
  <main class="login-page">
    <section class="login-card surface-card">
      <div class="login-mark" aria-hidden="true">FM</div>
      <span class="login-eyebrow">FreshMeal 配送端</span>
      <h1>骑手登录</h1>
      <p>账号由管理员创建，登录后可查看并处理配送任务。</p>

      <van-form class="login-form" @submit="login">
        <van-field
          v-model.trim="username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          autocomplete="username"
          :rules="usernameRules"
        />
        <van-field
          v-model="password"
          name="password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          clearable
          :rules="passwordRules"
        />
        <p v-if="errorMessage" class="form-error" role="alert">
          {{ errorMessage }}
        </p>
        <van-button
          block
          native-type="submit"
          type="primary"
          :loading="loading"
          :disabled="loading"
        >
          {{ loading ? '登录中' : '登录' }}
        </van-button>
      </van-form>

      <p class="account-note">账号由管理员创建，骑手端不提供注册入口。</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onScopeDispose, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { isApiError } from '@/types/api/common';
import { useRiderStore } from '@/store/modules/rider';
import { getSafeRedirect } from '@/utils/url';

const router = useRouter();
const route = useRoute();
const riderStore = useRiderStore();
const username = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const usernameRules = [{ required: true, message: '请输入账号' }];
const passwordRules = [
  { required: true, message: '请输入密码' },
  {
    validator: (value: string) =>
      (value.length >= 6 && value.length <= 100) || '密码长度应为6-100位',
  },
];

onScopeDispose(() => riderStore.cancelLogin());

const login = async (values: Record<string, string>) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await riderStore.login({
      username: values.username ?? username.value,
      password: values.password ?? password.value,
    });
    await router.replace(getSafeRedirect(route.query.redirect));
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
.login-page {
  display: grid;
  min-height: 100dvh;
  padding: calc(var(--space-5) + env(safe-area-inset-top)) var(--space-5)
    calc(var(--space-5) + env(safe-area-inset-bottom));
  background:
    radial-gradient(
      circle at 50% 0,
      var(--color-primary-soft),
      transparent 42%
    ),
    var(--color-background);
}

.login-card {
  align-self: start;
  max-width: 28rem;
  padding: var(--space-6);
  margin: 7vh auto 0;
}

.login-mark {
  display: grid;
  width: 3rem;
  height: 3rem;
  place-items: center;
  font-weight: 800;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-lg);
}

.login-eyebrow {
  display: block;
  margin-top: var(--space-5);
  font-size: 0.75rem;
  font-weight: 750;
  color: var(--color-primary);
}

h1 {
  margin: var(--space-2) 0 var(--space-2);
  font-size: var(--text-title);
  line-height: 1.25;
}

.login-card > p {
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
}

.login-form {
  display: grid;
  gap: var(--space-3);

  :deep(.van-field) {
    padding: var(--space-2) var(--space-3);
    background: var(--color-background);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  :deep(.van-field__label) {
    width: 3.5rem;
    font-weight: 650;
    color: var(--color-text-secondary);
  }

  .van-button {
    min-height: 2.875rem;
    margin-top: var(--space-2);
    font-weight: 700;
  }
}

.form-error {
  margin: 0;
  font-size: var(--text-secondary);
  color: var(--color-danger);
}

.account-note {
  margin-top: var(--space-5) !important;
  margin-bottom: 0 !important;
  font-size: 0.75rem;
  text-align: center;
}
</style>
