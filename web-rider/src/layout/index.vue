<template>
  <div class="app-shell">
    <header class="top-bar">
      <span class="top-bar__brand">FreshMeal 配送端</span>
      <span class="top-bar__title">{{ pageTitle }}</span>
      <span />
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
import { useRoute } from 'vue-router';

const route = useRoute();
const pageTitle = computed(
  () =>
    (typeof route.meta.title === 'string' ? route.meta.title : '') ||
    'FreshMeal 配送端',
);
</script>

<style scoped lang="scss">
.app-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100dvh;
  background: var(--color-background);
}

.top-bar {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  min-height: 3.25rem;
  padding-top: env(safe-area-inset-top);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.top-bar__brand {
  padding-left: var(--space-4);
  font-size: var(--text-secondary);
  font-weight: 700;
  color: var(--color-primary);
}

.top-bar__title {
  font-weight: 700;
  text-align: center;
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
  background: rgb(255 255 255 / 96%);
  border-top: 1px solid var(--color-border);
}

.tab-bar__item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  align-items: center;
  justify-content: center;
  min-width: 0;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  text-decoration: none;

  &.router-link-active {
    font-weight: 700;
    color: var(--color-primary);
  }
}

@media (min-width: 768px) {
  .tab-bar {
    right: 50%;
    left: auto;
    width: 24rem;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    transform: translateX(50%);
  }
}
</style>
