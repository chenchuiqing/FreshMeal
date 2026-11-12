<template>
  <div class="app-shell" :style="{ height: layoutHeight }">
    <header v-if="!route.meta.hideHeader" class="top-bar">
      <button
        v-if="!isRootPage"
        class="icon-button"
        type="button"
        aria-label="返回"
        @click="router.back()"
      >
        ←
      </button>
      <span v-else />
      <span class="top-bar__title">{{ pageTitle }}</span>
      <span />
    </header>

    <p v-if="!isOnline" class="offline-banner" role="status">
      网络已断开，恢复连接后可继续加载内容
    </p>

    <main class="page-scroll" :class="{ 'page-scroll--with-nav': isRootPage }">
      <RouterView v-slot="{ Component, route: childRoute }">
        <KeepAlive>
          <component
            :is="Component"
            v-if="childRoute.meta.keepAlive"
            :key="childRoute.name"
          />
        </KeepAlive>
        <component
          :is="Component"
          v-if="!childRoute.meta.keepAlive"
          :key="childRoute.fullPath"
        />
      </RouterView>
    </main>

    <nav v-if="isRootPage" class="tab-bar" aria-label="主导航">
      <RouterLink to="/home" class="tab-bar__brand">
        <SvgIcon name="logo" />
        <strong>FreshMeal</strong>
      </RouterLink>
      <RouterLink
        v-for="item in tabs"
        :key="item.to"
        :to="item.to"
        class="tab-bar__item"
      >
        <SvgIcon :name="item.icon" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useLayoutViewportHeight } from '@/composables/useLayoutViewportHeight';
import { useNetworkStatus } from '@/composables/useNetworkStatus';
import type { SvgIconName } from '@/types/icon';

const route = useRoute();
const router = useRouter();
const { isOnline } = useNetworkStatus();
const layoutHeight = useLayoutViewportHeight();
const rootNames = new Set(['home', 'catalog', 'cart', 'member']);
const isRootPage = computed(() => rootNames.has(String(route.name)));
const pageTitle = computed(
  () =>
    (typeof route.meta.title === 'string' ? route.meta.title : '') ||
    'FreshMeal',
);
const tabs = computed<Array<{ to: string; label: string; icon: SvgIconName }>>(
  () => [
    { to: '/home', label: '首页', icon: 'home' },
    { to: '/catalog', label: '菜单', icon: 'shop' },
    { to: '/cart', label: '购物车', icon: 'cart' },
    { to: '/member', label: '我的', icon: 'user' },
  ],
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
  grid-template-columns: 3.25rem 1fr 3.25rem;
  align-items: center;
  min-height: 3.25rem;
  padding-top: env(safe-area-inset-top);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.top-bar__title {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: var(--text-body);
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

.icon-button {
  width: var(--touch-target);
  min-height: var(--touch-target);
  font-size: 1.5rem;
  color: var(--color-text);
  background: transparent;
  border: 0;
}

.page-scroll {
  flex: 1;
  min-height: 0;
  overflow: hidden auto;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
}

.page-scroll--with-nav {
  padding-bottom: calc(4rem + env(safe-area-inset-bottom));
}

.offline-banner {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-secondary);
  color: var(--color-warning);
  text-align: center;
  background: var(--color-warning-soft);
}

.tab-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  min-height: 3.75rem;
  padding-bottom: env(safe-area-inset-bottom);
  background: color-mix(in srgb, var(--color-surface) 96%, transparent);
  border-top: 1px solid var(--color-border);
  backdrop-filter: blur(12px);
}

.tab-bar__brand {
  display: none;
}

.tab-bar__item {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  align-items: center;
  justify-content: center;
  min-width: 0;
  font-size: 0.6875rem;
  color: var(--color-text-secondary);
  text-decoration: none;

  .svg-icon {
    font-size: 1.25rem;
  }

  &.router-link-active {
    font-weight: 700;
    color: var(--color-primary);
  }
}

@media (min-width: 768px) {
  .tab-bar {
    inset: 0.75rem 1.5rem auto;
    grid-template-columns: auto repeat(4, auto);
    gap: var(--space-1);
    width: min(calc(100% - 3rem), var(--content-max-width));
    min-height: 3.25rem;
    padding: 0.25rem;
    margin: 0 auto;
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-float);
  }

  .tab-bar__brand {
    display: flex;
    gap: var(--space-2);
    align-items: center;
    align-self: stretch;
    padding: 0 var(--space-3);
    color: var(--color-text);
    text-decoration: none;

    .svg-icon {
      font-size: 1.25rem;
      color: var(--color-primary);
    }
  }

  .tab-bar__item {
    flex-direction: row;
    gap: var(--space-2);
    min-width: 5rem;
    padding: 0 var(--space-3);
    border-radius: var(--radius-md);

    &.router-link-active {
      color: var(--color-text);
      background: var(--color-background-soft);
    }

    .svg-icon {
      font-size: 1rem;
    }
  }
}

@supports not (height: 100dvh) {
  .app-shell {
    height: 100vh;
  }
}
</style>
