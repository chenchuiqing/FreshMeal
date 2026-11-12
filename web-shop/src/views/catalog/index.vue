<template>
  <div class="catalog-page page">
    <header class="page-header">
      <div>
        <span>今日菜单</span>
        <h1>按你的目标选择餐品</h1>
        <p>分类、价格、热量和健康标签将在商品接口接入后同步展示。</p>
      </div>
    </header>

    <div class="filter-row" aria-label="餐品筛选">
      <van-tag plain type="primary">低卡</van-tag>
      <van-tag plain type="primary">高蛋白</van-tag>
      <van-tag plain type="primary">均衡餐</van-tag>
      <van-tag plain>预约配送</van-tag>
    </div>

    <van-empty description="暂无可售餐品" image="default">
      <RouterLink class="empty-action" to="/home">返回首页</RouterLink>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CatalogPage' });
</script>

<style scoped lang="scss">
.catalog-page {
  padding-top: calc(var(--space-5) + env(safe-area-inset-top));
}

.page-header span {
  font-size: var(--text-caption);
  font-weight: 750;
  color: var(--color-primary);
}

.page-header h1 {
  margin: var(--space-2) 0;
  font-size: var(--text-page-title);
  font-weight: 780;
}

.page-header p {
  max-width: 36rem;
  margin: 0;
  color: var(--color-text-secondary);
}

.filter-row {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-5) 0;
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);

  :deep(.van-tag) {
    min-height: 1.875rem;
    padding: 0.25rem var(--space-3);
    white-space: nowrap;
  }
}

.empty-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--touch-target);
  padding: 0 var(--space-4);
  font-weight: 700;
  color: var(--color-primary-contrast);
  text-decoration: none;
  background: var(--color-primary);
  border-radius: var(--radius-md);
}
</style>
