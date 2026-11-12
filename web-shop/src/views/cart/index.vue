<template>
  <div class="cart-page page">
    <header class="page-header">
      <div>
        <span>购物车</span>
        <h1>确认餐品与整单热量</h1>
        <p>结算前可调整数量、规格、地址和预约配送时段。</p>
      </div>
    </header>

    <van-empty description="购物车还是空的">
      <RouterLink class="empty-action" to="/catalog">去选餐</RouterLink>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CartPage' });
</script>

<style scoped lang="scss">
.cart-page {
  padding-top: calc(var(--space-5) + env(safe-area-inset-top));
}

.page-header {
  margin-bottom: var(--space-5);
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
  margin: 0;
  color: var(--color-text-secondary);
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
