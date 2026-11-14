<template>
  <div class="collection-page page">
    <header class="page-header">
      <div>
        <span>我的清单</span>
        <h1>我的收藏</h1>
        <p>收藏的商品会保留在这里，方便下次快速下单。</p>
      </div>
      <RouterLink class="text-button" to="/member">返回我的</RouterLink>
    </header>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>收藏加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <van-empty v-else-if="!page?.records.length" description="还没有收藏商品">
      <RouterLink class="button-primary empty-action" to="/catalog">
        去逛逛
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
import { getFavoritePage } from '@/api/modules/engagement';
import { isApiError } from '@/types/api/common';

defineOptions({ name: 'FavoritesPage' });

const current = ref(1);
const size = 12;

const {
  data: page,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ['shop', 'favorite', 'page', current, size],
  queryFn: ({ signal }) =>
    getFavoritePage({ current: current.value, size }, signal),
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '收藏加载失败',
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
