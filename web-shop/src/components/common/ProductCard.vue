<template>
  <RouterLink
    class="product-card"
    :class="{ 'product-card--inactive': badge !== '' }"
    :to="{ name: 'productDetail', params: { id: product.id } }"
  >
    <span class="product-card__cover">
      <img :src="product.cover" :alt="product.name" loading="lazy" />
      <span v-if="badge" class="product-card__badge">{{ badge }}</span>
    </span>
    <span class="product-card__body">
      <b class="product-card__name">{{ product.name }}</b>
      <span v-if="product.subtitle" class="product-card__subtitle">{{
        product.subtitle
      }}</span>
      <span v-if="visibleTags.length > 0" class="product-card__tags">
        <van-tag
          v-for="tag in visibleTags"
          :key="tag"
          plain
          type="primary"
          >{{ tag }}</van-tag
        >
      </span>
      <span class="product-card__price">
        <b>{{ formatPrice(product.price) }}</b>
        <s v-if="originalPriceText">{{ originalPriceText }}</s>
      </span>
      <span class="product-card__foot">
        <span>{{ product.calories }} kcal</span>
        <span>已售 {{ product.salesCount }}</span>
      </span>
    </span>
  </RouterLink>
</template>

<script setup lang="ts">
import type { ShopProduct } from '@/api/modules/product';
import { formatPrice } from '@/utils/product';

const props = defineProps<{ product: ShopProduct }>();

const badge = computed(() => {
  if (props.product.status === 0) return '已下架';
  if (props.product.stock <= 0) return '售罄';
  return '';
});
const visibleTags = computed(() => props.product.healthTags.slice(0, 2));
const originalPriceText = computed(() =>
  typeof props.product.originalPrice === 'number'
    ? formatPrice(props.product.originalPrice)
    : '',
);
</script>

<style scoped lang="scss">
.product-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  color: var(--color-text);
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);

  &:hover {
    border-color: var(--color-border-strong);
  }
}

.product-card--inactive {
  opacity: 0.72;
  filter: grayscale(1);
}

.product-card__cover {
  position: relative;
  display: block;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--color-background-soft);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.product-card__badge {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  padding: 0 var(--space-2);
  font-size: var(--text-caption);
  font-weight: 700;
  line-height: 1.75rem;
  color: var(--color-text);
  background: var(--color-warning-soft);
  border: 1px solid
    color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
  border-radius: var(--radius-sm);
}

.product-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
}

.product-card__name {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: var(--text-card-title);
  font-weight: 700;
  line-height: var(--leading-tight);
  white-space: nowrap;
}

.product-card__subtitle {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.product-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.product-card__price {
  display: flex;
  gap: var(--space-2);
  align-items: baseline;
  margin-top: auto;

  b {
    font-size: var(--text-section-title);
    color: var(--color-danger);
  }

  s {
    font-size: var(--text-caption);
    color: var(--color-text-muted);
  }
}

.product-card__foot {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-caption);
  color: var(--color-text-muted);
}
</style>
