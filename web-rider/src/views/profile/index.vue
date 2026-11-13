<template>
  <section class="page">
    <header class="hero">
      <div class="hero__identity">
        <span class="avatar" aria-hidden="true">{{ avatarText }}</span>
        <div class="hero__name">
          <strong>{{ displayName }}</strong>
          <span>{{ riderStore.info?.username }}</span>
        </div>
        <span class="role-badge">配送员</span>
      </div>

      <dl class="hero__facts">
        <div>
          <dt>账号状态</dt>
          <dd>正常</dd>
        </div>
        <div>
          <dt>所属配送区域</dt>
          <dd>待分配</dd>
        </div>
      </dl>
    </header>

    <section class="panel">
      <h2 class="panel__title">账号信息</h2>
      <van-cell-group>
        <van-cell title="姓名" :value="riderStore.info?.realName || '-'" />
        <van-cell title="手机号" :value="riderStore.info?.phone || '-'" />
        <van-cell title="登录账号" :value="riderStore.info?.username || '-'" />
      </van-cell-group>
      <p class="panel__note">
        接单、取货上报、送达上报与业绩统计将在配送业务上线后开放。
      </p>
    </section>

    <p v-if="errorMessage" class="form-error" role="alert">
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
const displayName = computed(
  () => riderStore.info?.realName || riderStore.info?.username || '配送员',
);
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

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
  padding: 0 var(--space-4) calc(4.5rem + env(safe-area-inset-bottom));
}

/* 深色身份区：与顶栏连成一体 */
.hero {
  margin: 0 calc(-1 * var(--space-4));
  padding: var(--space-5) var(--space-4) var(--space-4);
  color: var(--color-ink-text);
  background: var(--color-ink);
  border-radius: 0 0 var(--radius-xl) var(--radius-xl);
}

.hero__identity {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.avatar {
  display: grid;
  width: 3.25rem;
  height: 3.25rem;
  flex: 0 0 auto;
  place-items: center;
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--color-brand-ink);
  background: var(--color-brand);
  border-radius: 50%;
}

.hero__name {
  min-width: 0;

  strong,
  span {
    display: block;
  }

  strong {
    font-size: var(--text-title);
    font-weight: 700;
  }

  span {
    font-size: var(--text-secondary);
    color: var(--color-ink-muted);
  }
}

.role-badge {
  margin-left: auto;
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-caption);
  font-weight: 700;
  color: var(--color-brand);
  border: 1px solid var(--color-brand);
  border-radius: var(--radius-pill);
}

.hero__facts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
  margin: var(--space-5) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-ink-line);

  dt {
    font-size: var(--text-caption);
    color: var(--color-ink-muted);
  }

  dd {
    margin: var(--space-1) 0 0;
    font-size: var(--text-body);
    font-weight: 700;
  }
}

.panel {
  padding: var(--space-4);
  background: var(--color-paper);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.panel__title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-section);
  font-weight: 700;
}

.panel__note {
  margin: var(--space-3) 0 0;
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
}

:deep(.van-cell-group) {
  background: transparent;
}

:deep(.van-cell) {
  padding-top: var(--space-3);
  padding-right: 0;
  padding-bottom: var(--space-3);
  padding-left: 0;
  background: transparent;
}

:deep(.van-cell::after) {
  left: 0;
}

:deep(.van-button--plain) {
  background: var(--color-paper);
  border-color: transparent;
}

.form-error {
  margin: 0;
  font-size: var(--text-secondary);
  color: var(--color-danger);
  text-align: center;
}
</style>
