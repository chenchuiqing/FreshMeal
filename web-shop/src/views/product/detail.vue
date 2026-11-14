<template>
  <div class="detail-page page">
    <div v-if="!validProductId" class="state-panel" role="alert">
      <p>商品链接无效或商品不存在</p>
      <RouterLink class="button-secondary" to="/catalog">返回菜单</RouterLink>
    </div>
    <div v-else-if="detailFailed" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <button type="button" class="button-primary" @click="retry">重新加载</button>
    </div>
    <div v-else-if="!detail" class="state-panel">
      <van-loading vertical>商品加载中…</van-loading>
    </div>
    <template v-else>
      <p v-if="unavailableText" class="unavailable-banner" role="status">
        {{ unavailableText }}
      </p>

      <div class="gallery">
        <van-swipe
          v-if="gallery.length > 0"
          :loop="gallery.length > 1"
          indicator-color="#ffffff"
        >
          <van-swipe-item
            v-for="(src, index) in gallery"
            :key="`${src}-${index}`"
          >
            <img class="gallery__image" :src="src" :alt="detail.name" />
          </van-swipe-item>
        </van-swipe>
        <div v-else class="gallery__placeholder">
          <SvgIcon name="logo" />
        </div>
      </div>

      <section class="summary">
        <p class="summary__price">
          <b>{{ formatPrice(currentPrice) }}</b>
          <s v-if="originalPriceText">{{ originalPriceText }}</s>
          <span class="summary__unit">/ {{ detail.unit }}</span>
        </p>
        <h1 class="summary__name">{{ detail.name }}</h1>
        <p v-if="detail.subtitle" class="summary__subtitle">
          {{ detail.subtitle }}
        </p>
        <div v-if="detail.healthTags.length > 0" class="summary__tags">
          <van-tag
            v-for="tag in detail.healthTags"
            :key="tag"
            plain
            type="primary"
            >{{ tag }}</van-tag
          >
        </div>
      </section>

      <section v-if="activeSpecs.length > 0" class="block">
        <h2 class="block__title">规格</h2>
        <van-radio-group v-model="selectedSpecId" class="option-list">
          <van-radio
            v-for="spec in activeSpecs"
            :key="spec.id"
            :name="spec.id"
            class="option-item"
          >
            <span class="option-item__label">{{ spec.name }}</span>
            <span class="option-item__meta">
              <b>{{ formatPrice(detail.price + spec.priceDelta) }}</b>
            </span>
          </van-radio>
        </van-radio-group>
      </section>

      <section v-for="group in activeGroups" :key="group.id" class="block">
        <h2 class="block__title">
          {{ group.name }}
          <span class="block__tag">{{
            group.required === 1 ? '必选' : '可选'
          }}</span>
          <span v-if="group.type === 2" class="block__tag">可多选</span>
        </h2>
        <p v-if="missingRequired.includes(group.name)" class="block__hint">
          请选择{{ group.name }}
        </p>
        <van-radio-group
          v-if="group.type === 1"
          v-model="radioSelections[group.id]"
          class="option-list"
        >
          <van-radio
            v-for="item in group.items"
            :key="item.id"
            :name="item.id"
            class="option-item"
          >
            <span class="option-item__label">{{ item.name }}</span>
            <span class="option-item__meta">
              <span v-if="item.calories > 0">+{{ item.calories }} kcal</span>
              <b v-if="item.priceDelta !== 0">{{ formatDelta(item.priceDelta) }}</b>
            </span>
          </van-radio>
        </van-radio-group>
        <van-checkbox-group
          v-else
          v-model="checkboxSelections[group.id]"
          class="option-list"
        >
          <van-checkbox
            v-for="item in group.items"
            :key="item.id"
            :name="item.id"
            shape="square"
            class="option-item"
          >
            <span class="option-item__label">{{ item.name }}</span>
            <span class="option-item__meta">
              <span v-if="item.calories > 0">+{{ item.calories }} kcal</span>
              <b v-if="item.priceDelta !== 0">{{ formatDelta(item.priceDelta) }}</b>
            </span>
          </van-checkbox>
        </van-checkbox-group>
      </section>

      <section class="block">
        <h2 class="block__title">营养信息</h2>
        <table class="nutrition">
          <tbody>
            <tr v-for="row in nutritionRows" :key="row.label">
              <th scope="row">{{ row.label }}</th>
              <td>{{ row.value }} {{ row.unit }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="detail.nutritionDesc" class="block__note">
          {{ detail.nutritionDesc }}
        </p>
      </section>

      <section class="block">
        <h2 class="block__title">商品信息</h2>
        <dl class="facts">
          <template v-for="row in factRows" :key="row.label">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </template>
        </dl>
      </section>

      <section v-if="detailParagraphs.length > 0" class="block">
        <h2 class="block__title">商品详情</h2>
        <div class="rich-text">
          <p v-for="(paragraph, index) in detailParagraphs" :key="index">
            {{ paragraph }}
          </p>
        </div>
      </section>

      <footer class="action-bar">
        <p class="action-bar__hint">
          单份 {{ currentCalories }} kcal · 服务端将在下单时再次校验库存
        </p>
        <van-button
          block
          type="primary"
          size="large"
          :loading="addingToCart"
          :disabled="!canAddToCart"
          loading-text="加入中…"
          @click="addSelectedToCart"
        >
          加入购物车
        </van-button>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import { getShopProductDetail } from '@/api/modules/product';
import { addCartItem } from '@/api/modules/trade';
import type { ProductOptionItem } from '@/api/modules/product';
import { isApiError } from '@/types/api/common';
import { useUserStore } from '@/store/modules/user';
import {
  collectDetailGallery,
  formatDelta,
  formatPrice,
  missingRequiredOptions,
  pickDefaultSpec,
  resolveUnavailableText,
} from '@/utils/product';

defineOptions({ name: 'ProductDetailPage' });

interface ActiveOptionGroup {
  id: number;
  name: string;
  type: number;
  required: number;
  items: ProductOptionItem[];
}

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const addingToCart = ref(false);
const rawId = Array.isArray(route.params.id)
  ? route.params.id[0]
  : route.params.id;
const productId = Number(rawId);
const validProductId = Number.isInteger(productId) && productId > 0;

const {
  data: detail,
  error: detailError,
  isLoading: detailLoading,
  isError: detailFailed,
  refetch: refetchDetail,
} = useQuery({
  queryKey: ['shop', 'product', productId],
  queryFn: ({ signal }) => getShopProductDetail(productId, signal),
  enabled: validProductId,
});

const radioSelections = ref<Record<number, number | undefined>>({});
const checkboxSelections = ref<Record<number, number[]>>({});
const selectedSpecId = ref<number | undefined>(undefined);

watch(
  detail,
  (value) => {
    radioSelections.value = {};
    const checkbox: Record<number, number[]> = {};
    for (const group of value?.options ?? []) {
      if (group.type === 2) checkbox[group.id] = [];
    }
    checkboxSelections.value = checkbox;
  },
  { immediate: true },
);

const activeSpecs = computed(
  () => (detail.value?.specs ?? []).filter((spec) => spec.status === 1),
);

watch(
  activeSpecs,
  (specs) => {
    selectedSpecId.value = pickDefaultSpec(specs)?.id;
  },
  { immediate: true },
);

const selectedSpec = computed(() =>
  activeSpecs.value.find((spec) => spec.id === selectedSpecId.value),
);

const activeGroups = computed<ActiveOptionGroup[]>(() =>
  (detail.value?.options ?? [])
    .filter((group) => group.type === 1 || group.type === 2)
    .map((group) => ({
      id: group.id,
      name: group.name,
      type: group.type,
      required: group.required,
      items: group.items.filter((item) => item.status === 1),
    }))
    .filter((group) => group.items.length > 0),
);

const missingRequired = computed(() =>
  missingRequiredOptions(detail.value?.options ?? [], {
    radio: radioSelections.value,
    checkbox: checkboxSelections.value,
  }),
);

const currentPrice = computed(() => {
  const product = detail.value;
  if (!product) return 0;
  let price = product.price + (selectedSpec.value?.priceDelta ?? 0);
  for (const group of activeGroups.value) {
    const selectedIds: number[] = [];
    if (group.type === 1) {
      const id = radioSelections.value[group.id];
      if (id !== undefined) selectedIds.push(id);
    } else {
      selectedIds.push(...(checkboxSelections.value[group.id] ?? []));
    }
    for (const item of group.items) {
      if (selectedIds.includes(item.id)) price += item.priceDelta;
    }
  }
  return price;
});

const currentCalories = computed(() => {
  const product = detail.value;
  if (!product) return 0;
  let calories = selectedSpec.value?.calories ?? product.calories;
  for (const group of activeGroups.value) {
    let selectedIds: number[] = checkboxSelections.value[group.id] ?? [];
    if (group.type === 1) {
      const selectedId = radioSelections.value[group.id];
      selectedIds = selectedId === undefined ? [] : [selectedId];
    }
    calories += group.items
      .filter((item) => selectedIds.includes(item.id))
      .reduce((total, item) => total + item.calories, 0);
  }
  return calories;
});

const selectedOptionIds = computed(() => {
  const ids: number[] = [];
  for (const group of activeGroups.value) {
    if (group.type === 1) {
      const id = radioSelections.value[group.id];
      if (id !== undefined) ids.push(id);
    } else {
      ids.push(...(checkboxSelections.value[group.id] ?? []));
    }
  }
  return ids;
});

const canAddToCart = computed(
  () =>
    Boolean(
      detail.value?.available &&
        selectedSpec.value &&
        missingRequired.value.length === 0 &&
        !addingToCart.value,
    ),
);

const gallery = computed(() =>
  detail.value ? collectDetailGallery(detail.value) : [],
);
const unavailableText = computed(() =>
  detail.value ? resolveUnavailableText(detail.value) : null,
);
const originalPriceText = computed(() => {
  const product = detail.value;
  if (!product || typeof product.originalPrice !== 'number') return '';
  return formatPrice(product.originalPrice);
});

const nutritionRows = computed(() => {
  const product = detail.value;
  if (!product) return [];
  return [
    { label: '热量', value: product.calories, unit: 'kcal' },
    { label: '蛋白质', value: product.protein, unit: 'g' },
    { label: '脂肪', value: product.fat, unit: 'g' },
    { label: '碳水化合物', value: product.carbohydrate, unit: 'g' },
    { label: '钠', value: product.sodium, unit: 'mg' },
  ];
});

const factRows = computed(() => {
  const product = detail.value;
  if (!product) return [];
  const rows: Array<{ label: string; value: string }> = [
    {
      label: '过敏原',
      value:
        product.allergens.length > 0
          ? product.allergens.join('、')
          : '不含常见过敏原',
    },
  ];
  if (product.storageLabel) {
    rows.push({ label: '储存方式', value: product.storageLabel });
  }
  if (product.shelfLifeDays > 0) {
    rows.push({ label: '保质期', value: `${product.shelfLifeDays} 天` });
  }
  if (product.producer) {
    rows.push({ label: '生产商', value: product.producer });
  }
  if (product.heatingMethod) {
    rows.push({ label: '食用方式', value: product.heatingMethod });
  }
  return rows;
});

const detailParagraphs = computed(() => {
  const text = detail.value?.detail ?? '';
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
});

const errorText = computed(() =>
  isApiError(detailError.value)
    ? detailError.value.message
    : '商品加载失败，请稍后重试',
);

function retry() {
  void refetchDetail();
}

async function addSelectedToCart() {
  const product = detail.value;
  if (!product || !selectedSpec.value || !canAddToCart.value) return;
  if (!userStore.token) {
    await router.push({
      name: 'login',
      query: { redirect: route.fullPath },
    });
    return;
  }
  addingToCart.value = true;
  try {
    await addCartItem({
      productId: product.id,
      specId: selectedSpec.value.id,
      optionItemIds: selectedOptionIds.value,
      quantity: 1,
    });
    showToast({ message: '已加入购物车', icon: 'success' });
  } catch (error) {
    showToast(
      isApiError(error) ? error.message : '加入购物车失败，请稍后重试',
    );
  } finally {
    addingToCart.value = false;
  }
}
</script>

<style scoped lang="scss">
.detail-page {
  padding-bottom: 0;
}

.unavailable-banner {
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-secondary);
  font-weight: 700;
  color: var(--color-danger);
  text-align: center;
  background: var(--color-danger-soft);
  border: 1px solid
    color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
}

.gallery {
  position: relative;
  aspect-ratio: 1;
  margin-top: var(--space-4);
  overflow: hidden;
  background: var(--color-background-soft);
  border-radius: var(--radius-lg);

  :deep(.van-swipe) {
    height: 100%;
  }
}

.gallery__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery__placeholder {
  display: grid;
  place-items: center;
  height: 100%;
  font-size: 3rem;
  color: var(--color-primary);
}

.summary {
  padding-top: var(--space-4);
}

.summary__price {
  display: flex;
  gap: var(--space-2);
  align-items: baseline;
  margin: 0;

  b {
    font-size: 1.75rem;
    color: var(--color-danger);
  }

  s {
    font-size: var(--text-secondary);
    color: var(--color-text-muted);
  }
}

.summary__unit {
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
}

.summary__name {
  margin: var(--space-2) 0 0;
  font-size: var(--text-page-title);
  font-weight: 780;
  line-height: var(--leading-tight);
  letter-spacing: -0.025em;
}

.summary__subtitle {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
}

.summary__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding-top: var(--space-3);
}

.block {
  padding-top: var(--space-5);
  margin-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

.block__title {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  margin: 0 0 var(--space-3);
  font-size: var(--text-card-title);
  font-weight: 700;
}

.block__tag {
  padding: 0 var(--space-2);
  font-size: var(--text-caption);
  font-weight: 650;
  line-height: 1.5rem;
  color: var(--color-warning);
  background: var(--color-warning-soft);
  border-radius: var(--radius-sm);
}

.block__hint {
  margin: 0 0 var(--space-2);
  font-size: var(--text-secondary);
  color: var(--color-text-muted);
}

.block__note {
  margin: var(--space-3) 0 0;
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
}

.option-list {
  display: grid;
  gap: var(--space-2);
}

.option-item {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  min-height: var(--touch-target);
  padding: var(--space-3);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  :deep(.van-radio__label),
  :deep(.van-checkbox__label) {
    display: flex;
    flex: 1;
    gap: var(--space-3);
    align-items: center;
    justify-content: space-between;
  }
}

.option-item__label {
  font-weight: 600;
}

.option-item__meta {
  display: flex;
  gap: var(--space-2);
  align-items: baseline;
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);

  b {
    font-size: var(--text-body);
    color: var(--color-danger);
  }
}

.nutrition {
  width: 100%;
  font-size: var(--text-secondary);
  border-collapse: collapse;

  th,
  td {
    padding: var(--space-2) 0;
    border-bottom: 1px solid var(--color-border);
  }

  th {
    font-weight: 600;
    color: var(--color-text-secondary);
    text-align: left;
  }

  td {
    font-variant-numeric: tabular-nums;
    color: var(--color-text);
    text-align: right;
  }

  tr:last-child th,
  tr:last-child td {
    border-bottom: 0;
  }
}

.facts {
  display: grid;
  grid-template-columns: 5.5rem minmax(0, 1fr);
  gap: var(--space-2) var(--space-3);
  margin: 0;
  font-size: var(--text-secondary);

  dt {
    color: var(--color-text-secondary);
  }

  dd {
    margin: 0;
    color: var(--color-text);
  }
}

.rich-text {
  display: grid;
  gap: var(--space-3);
  max-width: var(--reading-max-width);
  color: var(--color-text-secondary);

  p {
    margin: 0;
  }
}

.action-bar {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3) 0 calc(var(--space-3) + env(safe-area-inset-bottom));
  margin-top: var(--space-6);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.action-bar__hint {
  margin: 0;
  font-size: var(--text-secondary);
  color: var(--color-text-muted);
  text-align: center;
}
</style>
