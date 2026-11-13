<template>
  <section class="page">
    <header class="profile-card surface-card">
      <div class="avatar" aria-hidden="true">
        {{ avatarText }}
      </div>
      <div>
        <strong>{{ riderStore.info?.realName || '配送员' }}</strong>
        <span>{{ riderStore.info?.username }}</span>
      </div>
    </header>

    <van-cell-group inset title="账号信息">
      <van-cell title="姓名" :value="riderStore.info?.realName || '-'" />
      <van-cell title="手机号" :value="riderStore.info?.phone || '-'" />
      <van-cell title="角色" value="rider" />
    </van-cell-group>

    <p v-if="errorMessage" class="logout-error" role="alert">
      {{ errorMessage }}
    </p>
    <van-button
      block
      type="danger"
      plain
      :loading="loading"
      :disabled="loading"
      @click="logout"
    >
      {{ loading ? '退出中' : '退出登录' }}
    </van-button>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useRiderStore } from '@/store/modules/rider';
import { isApiError } from '@/types/api/common';

const router = useRouter();
const riderStore = useRiderStore();
const loading = ref(false);
const errorMessage = ref('');
const avatarText = computed(() =>
  (riderStore.info?.realName || riderStore.info?.username || '骑')
    .slice(0, 1)
    .toUpperCase(),
);

const logout = async () => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await riderStore.logout();
    await router.replace({ name: 'login' });
  } catch (error) {
    errorMessage.value = isApiError(error)
      ? error.message
      : '退出登录失败，请稍后重试';
    if (!riderStore.token) await router.replace({ name: 'login' });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.page {
  display: grid;
  gap: var(--space-4);
  width: min(100%, 48rem);
  padding: var(--space-4);
  margin: 0 auto;
}

.profile-card {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  background:
    linear-gradient(135deg, var(--color-primary-soft), #fff 70%),
    var(--color-surface);

  strong,
  span {
    display: block;
  }

  span {
    color: var(--color-text-secondary);
  }
}

.avatar {
  display: grid;
  width: 3.5rem;
  height: 3.5rem;
  flex: 0 0 auto;
  place-items: center;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: 50%;
}

:deep(.van-cell-group--inset) {
  overflow: hidden;
  border-radius: var(--radius-lg);
}

.logout-error {
  margin: 0;
  color: var(--color-danger);
  text-align: center;
}
</style>
