<template>
  <div class="app-shell">
    <header class="rider-bar">
      <span class="brand-mark" aria-hidden="true">FM</span>
      <span class="brand-name">FreshMeal 配送端</span>
      <RouterLink to="/profile" class="rider-entry" aria-label="进入我的页面">
        <span class="rider-entry__avatar" aria-hidden="true">
          {{ avatarText }}
        </span>
      </RouterLink>
    </header>

    <main class="page-scroll">
      <RouterView />
    </main>

    <nav class="tab-bar" aria-label="主导航">
      <RouterLink to="/tasks" class="tab-bar__item">
        <van-icon name="orders-o" size="1.35rem" />
        <span>任务</span>
      </RouterLink>
      <RouterLink to="/profile" class="tab-bar__item">
        <van-icon name="user-o" size="1.35rem" />
        <span>我的</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRiderStore } from '@/store/modules/rider';

const riderStore = useRiderStore();
const avatarText = computed(() =>
  (riderStore.info?.realName || riderStore.info?.username || '骑')
    .slice(0, 1)
    .toUpperCase(),
);
</script>

<style scoped lang="scss">
.app-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100dvh;
  background: var(--color-canvas);
}

.rider-bar {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  min-height: 3.25rem;
  padding: env(safe-area-inset-top) var(--space-4) 0;
  color: var(--color-ink-text);
  background: var(--color-ink);
}

.brand-mark {
  display: grid;
  width: 1.75rem;
  height: 1.75rem;
  flex: 0 0 auto;
  place-items: center;
  font-size: 0.8125rem;
  font-weight: 800;
  color: var(--color-brand-ink);
  background: var(--color-brand);
  border-radius: var(--radius-sm);
}

.brand-name {
  font-size: var(--text-body);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.rider-entry {
  margin-left: auto;
  text-decoration: none;
  border-radius: var(--radius-pill);
}

.rider-entry__avatar {
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  font-size: var(--text-secondary);
  font-weight: 700;
  color: var(--color-brand-ink);
  background: var(--color-brand);
  border-radius: 50%;
}

.page-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden auto;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
}

.tab-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  min-height: 3.75rem;
  padding-bottom: env(safe-area-inset-bottom);
  background: var(--color-ink);
  border-top: 1px solid var(--color-ink-line);
}

.tab-bar__item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  align-items: center;
  justify-content: center;
  min-width: 0;
  font-size: var(--text-caption);
  color: var(--color-ink-muted);
  text-decoration: none;

  &.router-link-active {
    font-weight: 700;
    color: var(--color-brand);
  }
}

@media (min-width: 768px) {
  .tab-bar {
    right: 50%;
    left: auto;
    width: 24rem;
    overflow: hidden;
    border: 1px solid var(--color-ink-line);
    border-bottom: 0;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    transform: translateX(50%);
  }
}
</style>
