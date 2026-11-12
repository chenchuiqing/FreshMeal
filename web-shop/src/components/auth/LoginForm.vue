<template>
  <van-form class="login-form" @submit="submit">
    <van-field
      v-model.trim="username"
      name="username"
      label="账号"
      placeholder="请输入账号"
      autocomplete="username"
      :rules="rules.username"
    />
    <van-field
      v-model="password"
      name="password"
      label="密码"
      type="password"
      placeholder="请输入密码"
      autocomplete="current-password"
      clearable
      :rules="rules.password"
    />
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
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
</template>

<script setup lang="ts">
import type { ShopAuthRequest } from '@/api/modules/auth';

const props = defineProps<{ loading?: boolean; error?: string }>();
const emit = defineEmits<{ submit: [params: ShopAuthRequest] }>();
const username = ref('');
const password = ref('');
const rules = {
  username: [{ required: true, message: '请输入账号' }],
  password: [
    { required: true, message: '请输入密码' },
    {
      validator: (value: string) =>
        (value.length >= 6 && value.length <= 100) || '密码长度应为6-100位',
    },
  ],
};

const submit = (values: Record<string, string>) => {
  if (props.loading) return;
  emit('submit', {
    username: values.username ?? username.value,
    password: values.password ?? password.value,
  });
};
</script>

<style scoped lang="scss">
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
</style>
