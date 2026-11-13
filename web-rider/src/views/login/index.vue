<template>
  <main class="login-page">
    <section class="login-brand">
      <span class="brand-mark" aria-hidden="true">FM</span>
      <div>
        <strong>FreshMeal 配送端</strong>
        <p>接单、送达、收入，一路都在线</p>
      </div>
    </section>

    <section class="login-card">
      <h1>骑手登录</h1>
      <p class="login-hint">使用管理员分配的账号登录</p>

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
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  color: var(--color-ink-text);
  background: var(--color-ink);
}

.login-brand {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  padding: calc(3.5rem + env(safe-area-inset-top)) var(--space-5) var(--space-6);

  strong {
    font-size: var(--text-title);
    font-weight: 700;
    letter-spacing: 0.02em;
  }

  p {
    margin: var(--space-1) 0 0;
    font-size: var(--text-secondary);
    color: var(--color-ink-muted);
  }
}

.brand-mark {
  display: grid;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  place-items: center;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-brand-ink);
  background: var(--color-brand);
  border-radius: var(--radius-md);
}

/* 登录表单作为底部面板浮在深色背景上 */
.login-card {
  padding: var(--space-6) var(--space-5)
    calc(var(--space-6) + env(safe-area-inset-bottom));
  margin-top: auto;
  color: var(--color-text);
  background: var(--color-paper);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

h1 {
  margin: 0;
  font-size: var(--text-title);
  font-weight: 800;
}

.login-hint {
  margin: var(--space-2) 0 var(--space-5);
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
}

.login-form {
  display: grid;
  gap: var(--space-3);

  :deep(.van-field) {
    padding: var(--space-3);
    background: var(--color-canvas);
    border: 1px solid transparent;
    border-radius: var(--radius-md);
  }

  :deep(.van-field:focus-within) {
    border-color: var(--color-brand);
  }

  :deep(.van-field__label) {
    width: 3rem;
    font-weight: 700;
  }

  .van-button {
    min-height: 3rem;
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
  margin: var(--space-5) 0 0;
  font-size: var(--text-caption);
  color: var(--color-text-muted);
  text-align: center;
}
</style>
