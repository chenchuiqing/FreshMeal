<template>
  <section class="page">
    <header class="welcome-card surface-card">
      <span>你好，{{ riderName }}</span>
      <strong>今天也辛苦了</strong>
      <p>任务数据将在配送业务接入后显示。</p>
    </header>

    <div class="state-panel">
      <van-icon name="logistics" size="2.5rem" color="#e86a33" />
      <strong>暂无待接任务</strong>
      <span>配送任务接口将在下一步接入。</span>
    </div>

    <section class="status-card surface-card">
      <div>
        <span>账号状态</span>
        <strong>由管理员创建</strong>
      </div>
      <div>
        <span>登录角色</span>
        <strong>rider</strong>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRiderStore } from '@/store/modules/rider';

const riderStore = useRiderStore();
const riderName = computed(
  () => riderStore.info?.realName || riderStore.info?.username || '骑手',
);
</script>

<style scoped lang="scss">
.page {
  display: grid;
  gap: var(--space-4);
  width: min(100%, 48rem);
  padding: var(--space-4);
  margin: 0 auto;
}

.welcome-card {
  background:
    linear-gradient(135deg, var(--color-primary-soft), #fff 72%),
    var(--color-surface);

  span,
  strong {
    display: block;
  }

  span {
    font-size: var(--text-secondary);
    color: var(--color-text-secondary);
  }

  strong {
    margin-top: var(--space-1);
    font-size: 1.25rem;
  }

  p {
    margin: var(--space-2) 0 0;
    color: var(--color-text-secondary);
  }
}

.status-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);

  span,
  strong {
    display: block;
  }

  span {
    font-size: var(--text-secondary);
    color: var(--color-text-secondary);
  }

  strong {
    margin-top: var(--space-1);
  }
}
</style>
