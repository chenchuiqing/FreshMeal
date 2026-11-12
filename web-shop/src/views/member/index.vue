<template>
  <div class="member-page page">
    <template v-if="!userStore.token">
      <section class="guest-card surface-card">
        <span class="avatar"><SvgIcon name="user" /></span>
        <h1>登录 FreshMeal</h1>
        <p>登录后查看订单、配送进度和账号资料。</p>
        <div class="guest-actions">
          <RouterLink class="primary-action" to="/login">登录</RouterLink>
          <RouterLink class="secondary-action" to="/register">注册</RouterLink>
        </div>
      </section>
      <p v-if="errorMessage" class="form-error" role="alert">
        {{ errorMessage }}
      </p>
    </template>

    <template v-else>
      <section class="profile-card surface-card">
        <span class="avatar"><SvgIcon name="user" /></span>
        <div>
          <small>当前账号</small>
          <h1>{{ displayName }}</h1>
          <p v-if="expiresText">登录有效期至 {{ expiresText }}</p>
        </div>
      </section>

      <section class="account-section">
        <h2>账号</h2>
        <div class="account-row">
          <span>账号名称</span>
          <strong>{{ displayName }}</strong>
        </div>
        <div class="account-row">
          <span>登录状态</span>
          <strong>已登录</strong>
        </div>
      </section>

      <van-button
        block
        plain
        type="danger"
        :loading="logoutLoading"
        @click="logout"
      >
        退出登录
      </van-button>
      <p v-if="errorMessage" class="form-error" role="alert">
        {{ errorMessage }}
      </p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/modules/user';
import { isApiError } from '@/types/api/common';

defineOptions({ name: 'MemberPage' });
const router = useRouter();
const userStore = useUserStore();
const logoutLoading = ref(false);
const errorMessage = ref('');
const displayName = computed(
  () =>
    userStore.info?.nickname || userStore.info?.username || 'FreshMeal 用户',
);
const expiresText = computed(() => {
  const raw = userStore.info?.expires;
  if (!raw) return '';
  const date = new Date(raw);
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleString('zh-CN', { hour12: false });
});

const logout = async () => {
  if (logoutLoading.value) return;
  logoutLoading.value = true;
  errorMessage.value = '';
  try {
    await userStore.logout();
    await router.replace('/home');
  } catch (error) {
    errorMessage.value = isApiError(error)
      ? error.message
      : '退出登录失败，请稍后重试';
  } finally {
    logoutLoading.value = false;
  }
};
</script>

<style scoped lang="scss">
.member-page {
  display: grid;
  gap: var(--space-4);
  align-content: start;
  padding-top: calc(var(--space-5) + env(safe-area-inset-top));
}

.guest-card,
.profile-card {
  display: grid;
  gap: var(--space-3);
  max-width: 36rem;
  padding: var(--space-6);
  margin-top: var(--space-3);
}

.avatar {
  display: grid;
  place-items: center;
  width: 3.25rem;
  height: 3.25rem;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  .svg-icon {
    font-size: 1.5rem;
  }
}

.guest-card h1,
.profile-card h1 {
  margin: var(--space-2) 0 0;
  font-size: var(--text-section-title);
  font-weight: 780;
}

.guest-card p,
.profile-card p {
  margin: 0;
  color: var(--color-text-secondary);
}

.profile-card {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.profile-card small {
  font-weight: 700;
  color: var(--color-success);
}

.guest-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-2);

  a {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 7rem;
    min-height: var(--touch-target);
    font-weight: 700;
    text-decoration: none;
    border-radius: var(--radius-md);
  }
}

.primary-action {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
}

.secondary-action {
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.account-section {
  max-width: 36rem;
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  h2 {
    margin: 0 0 var(--space-3);
    font-size: var(--text-card-title);
    font-weight: 750;
  }
}

.account-row {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) 0;
  border-top: 1px solid var(--color-border);

  span {
    color: var(--color-text-secondary);
  }
}

.member-page > .van-button {
  max-width: 36rem;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
