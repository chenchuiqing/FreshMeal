<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/home">← 返回首页</RouterLink>
    <section class="auth-card surface-card">
      <SvgIcon name="logo" />
      <span>FreshMeal 用户端</span>
      <h1>创建账号</h1>
      <p>注册成功后会自动登录，可以直接开始选择餐品。</p>
      <van-form class="register-form" @submit="register">
        <van-field
          v-model.trim="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          autocomplete="username"
          :rules="rules.username"
        />
        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          type="password"
          placeholder="6-100位"
          autocomplete="new-password"
          clearable
          :rules="rules.password"
        />
        <van-field
          v-model="form.confirmPassword"
          name="confirmPassword"
          label="确认密码"
          type="password"
          placeholder="再次输入密码"
          autocomplete="new-password"
          clearable
          :rules="rules.confirmPassword"
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
          {{ loading ? '注册中' : '注册并登录' }}
        </van-button>
      </van-form>
      <p class="auth-switch">
        已有账号？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/modules/user';
import { isApiError } from '@/types/api/common';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const errorMessage = ref('');
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});
const rules = {
  username: [{ required: true, message: '请输入账号' }],
  password: [
    { required: true, message: '请输入密码' },
    {
      validator: (value: string) =>
        (value.length >= 6 && value.length <= 100) || '密码长度应为6-100位',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: (value: string) =>
        value === form.password || '两次输入的密码不一致',
    },
  ],
};

const register = async (values: Record<string, string>) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await userStore.register({
      username: values.username ?? form.username,
      password: values.password ?? form.password,
    });
    await router.replace('/member');
  } catch (error) {
    errorMessage.value = isApiError(error)
      ? error.message
      : '注册失败，请稍后重试';
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
  margin: 3vh auto 0;

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

.register-form {
  display: grid;
  gap: var(--space-3);

  :deep(.van-field) {
    padding: var(--space-2) var(--space-3);
    background: var(--color-background);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  :deep(.van-field__label) {
    width: 4.5rem;
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
