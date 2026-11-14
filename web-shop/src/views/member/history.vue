<template>
  <div class="collection-page page">
    <header class="page-header">
      <div>
        <span>最近看过</span>
        <h1>浏览历史</h1>
        <p>按最近查看时间排列，帮助你找回心动的餐品。</p>
      </div>
      <RouterLink class="text-button" to="/member">返回我的</RouterLink>
    </header>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>浏览历史加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <van-empty v-else-if="!page?.records.length" description="还没有浏览记录">
      <RouterLink class="button-primary empty-action" to="/catalog">
        去发现商品
      </RouterLink>
    </van-empty>
    <template v-else>
      <div class="product-grid">
        <ProductCard
          v-for="product in page.records"
          :key="product.id"
          :product="product"
        />
      </div>
      <van-pagination
        v-if="page.pages > 1"
        v-model="current"
        :total-items="page.total"
        :items-per-page="size"
        mode="simple"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import ProductCard from '@/components/common/ProductCard.vue';
import { getBrowsingHistoryPage } from '@/api/modules/engagement';
import { isApiError } from '@/types/api/common';

defineOptions({ name: 'BrowsingHistoryPage' });

const current = ref(1);
const size = 12;

const {
  data: page,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ['shop', 'history', 'page', current, size],
  queryFn: ({ signal }) =>
    getBrowsingHistoryPage({ current: current.value, size }, signal),
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '浏览历史加载失败',
);
</script>

<style scoped lang="scss">
.collection-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.empty-action {
  margin-top: var(--space-3);
}

@media (min-width: 640px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
