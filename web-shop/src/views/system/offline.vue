<template>
  <main class="offline-page">
    <SvgIcon name="logo" />
    <h1>网络不可用</h1>
    <p>请检查网络连接，恢复后返回 FreshMeal 继续浏览。</p>
    <RouterLink to="/home">返回首页</RouterLink>
  </main>
</template>

<style scoped lang="scss">
.offline-page {
  display: grid;
  place-content: center;
  min-height: 100dvh;
  padding: var(--space-8);
  text-align: center;

  .svg-icon {
    margin: 0 auto;
    font-size: 2rem;
    color: var(--color-primary);
  }

  h1 {
    margin: var(--space-5) 0 var(--space-2);
    font-size: var(--text-page-title);
    font-weight: 780;
  }

  p {
    max-width: 30rem;
    line-height: 1.6;
    color: var(--color-text-secondary);
  }

  a {
    display: grid;
    place-items: center;
    min-height: var(--touch-target);
    padding: 0 var(--space-4);
    margin-top: var(--space-5);
    font-weight: 700;
    color: var(--color-primary-contrast);
    text-decoration: none;
    background: var(--color-primary);
    border-radius: var(--radius-md);
  }
}
</style>
