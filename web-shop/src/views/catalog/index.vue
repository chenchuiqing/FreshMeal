<template>
  <div class="catalog-page page">
    <header class="page-header">
      <div>
        <span>今日菜单</span>
        <h1>按你的目标选择餐品</h1>
        <p>搜索、分类、热量、价格与健康标签可组合筛选。</p>
      </div>
    </header>

    <van-search
      v-model="searchText"
      class="catalog-search"
      shape="round"
      placeholder="搜索餐品名称"
      clearable
      @search="applyKeyword"
      @clear="clearKeyword"
    />

    <nav class="category-nav" aria-label="餐品分类">
      <van-loading v-if="categoryLoading" class="category-nav__loading">
        分类加载中
      </van-loading>
      <template v-else>
        <div class="chip-row">
          <button
            type="button"
            class="chip"
            :class="{ 'chip--active': filters.categoryId === undefined }"
            :aria-pressed="filters.categoryId === undefined"
            @click="selectCategory(undefined)"
          >
            全部
          </button>
          <button
            v-for="item in roots"
            :key="item.id"
            type="button"
            class="chip"
            :class="{ 'chip--active': filters.categoryId === item.id }"
            :aria-pressed="filters.categoryId === item.id"
            @click="selectCategory(item.id)"
          >
            {{ item.name }}
          </button>
        </div>
        <div v-if="activeRoot && subCategories.length > 0" class="chip-row">
          <button
            type="button"
            class="chip chip--sub"
            :class="{ 'chip--active': filters.categoryId === activeRoot.id }"
            :aria-pressed="filters.categoryId === activeRoot.id"
            @click="selectCategory(activeRoot.id)"
          >
            全部{{ activeRoot.name }}
          </button>
          <button
            v-for="child in subCategories"
            :key="child.id"
            type="button"
            class="chip chip--sub"
            :class="{ 'chip--active': filters.categoryId === child.id }"
            :aria-pressed="filters.categoryId === child.id"
            @click="selectCategory(child.id)"
          >
            {{ child.name }}
          </button>
        </div>
      </template>
    </nav>

    <div class="toolbar">
      <div class="sort-tabs" role="group" aria-label="排序方式">
        <button
          v-for="option in SORT_OPTIONS"
          :key="option.value"
          type="button"
          class="sort-tab"
          :class="{ 'sort-tab--active': filters.sort === option.value }"
          :aria-pressed="filters.sort === option.value"
          @click="filters.sort = option.value"
        >
          {{ option.label }}
        </button>
      </div>
      <button
        type="button"
        class="filter-trigger"
        :class="{ 'filter-trigger--active': panelFilterCount > 0 }"
        :aria-expanded="panelShow"
        @click="panelShow = true"
      >
        筛选
        <span v-if="panelFilterCount > 0" class="filter-trigger__badge">{{
          panelFilterCount
        }}</span>
      </button>
    </div>

    <section class="results" aria-label="餐品列表" :aria-busy="productsLoading">
      <div v-if="productsLoading" class="state-panel">
        <van-loading vertical>餐品加载中…</van-loading>
      </div>
      <div v-else-if="productsFailed" class="state-panel" role="alert">
        <p>{{ errorMessage }}</p>
        <button type="button" class="button-primary" @click="retry">
          重新加载
        </button>
      </div>
      <van-empty v-else-if="products.length === 0" :description="emptyDescription">
        <button
          v-if="hasConditions"
          type="button"
          class="button-secondary empty-action"
          @click="clearConditions"
        >
          清除筛选条件
        </button>
      </van-empty>
      <van-list
        v-else
        v-model:loading="listLoading"
        v-model:error="listError"
        :finished="finished"
        finished-text="没有更多了"
        error-text="加载失败，点击重试"
        @load="loadMore"
      >
        <div class="product-grid">
          <ProductCard v-for="item in products" :key="item.id" :product="item" />
        </div>
      </van-list>
    </section>

    <van-popup
      v-model:show="panelShow"
      class="catalog-filter-popup"
      position="bottom"
      round
    >
      <div class="filter-panel">
        <header class="filter-panel__header">
          <h2>筛选与排序</h2>
          <button
            type="button"
            class="filter-panel__close"
            aria-label="关闭筛选面板"
            @click="panelShow = false"
          >
            ×
          </button>
        </header>
        <div class="filter-panel__body">
          <section class="filter-group">
            <h3>热量区间</h3>
            <div class="chip-row">
              <button
                v-for="preset in CALORIE_PRESETS"
                :key="preset.key"
                type="button"
                class="chip"
                :class="{ 'chip--active': filters.calorie === preset.key }"
                :aria-pressed="filters.calorie === preset.key"
                @click="toggleCalorie(preset.key)"
              >
                {{ preset.label }}
              </button>
            </div>
          </section>
          <section class="filter-group">
            <h3>价格区间</h3>
            <div class="chip-row">
              <button
                v-for="preset in PRICE_PRESETS"
                :key="preset.key"
                type="button"
                class="chip"
                :class="{ 'chip--active': filters.price === preset.key }"
                :aria-pressed="filters.price === preset.key"
                @click="togglePrice(preset.key)"
              >
                {{ preset.label }}
              </button>
            </div>
          </section>
          <section v-if="(healthTagOptions ?? []).length > 0" class="filter-group">
            <h3>
              健康标签
              <span class="filter-group__hint">多选取交集</span>
            </h3>
            <div class="chip-row">
              <button
                v-for="tag in healthTagOptions ?? []"
                :key="tag"
                type="button"
                class="chip"
                :class="{ 'chip--active': filters.tags.includes(tag) }"
                :aria-pressed="filters.tags.includes(tag)"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </section>
        </div>
        <footer class="filter-panel__footer">
          <span class="filter-panel__total">共 {{ total }} 件</span>
          <button type="button" class="button-secondary" @click="clearPanelFilters">
            重置
          </button>
          <button type="button" class="button-primary" @click="panelShow = false">
            确定
          </button>
        </footer>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { useInfiniteQuery, useQuery } from '@tanstack/vue-query';
import ProductCard from '@/components/common/ProductCard.vue';
import {
  getHealthTags,
  getShopCategoryTree,
  getShopProductPage,
} from '@/api/modules/product';
import type { ShopProductPageParams, ShopProductSort } from '@/api/modules/product';
import { isApiError } from '@/types/api/common';

defineOptions({ name: 'CatalogPage' });

type CalorieKey = 'le400' | 'mid400' | 'gt600';
type PriceKey = 'le20' | 'mid20' | 'gt40';

interface FilterPreset<K> {
  key: K;
  label: string;
  range: ShopProductPageParams;
}

const CALORIE_PRESETS: FilterPreset<CalorieKey>[] = [
  { key: 'le400', label: '≤400 kcal', range: { maxCalories: 400 } },
  {
    key: 'mid400',
    label: '400-600 kcal',
    range: { minCalories: 400, maxCalories: 600 },
  },
  { key: 'gt600', label: '>600 kcal', range: { minCalories: 601 } },
];

const PRICE_PRESETS: FilterPreset<PriceKey>[] = [
  { key: 'le20', label: '¥20 以下', range: { maxPrice: 20 } },
  { key: 'mid20', label: '¥20-40', range: { minPrice: 20, maxPrice: 40 } },
  { key: 'gt40', label: '¥40 以上', range: { minPrice: 40.01 } },
];

const SORT_OPTIONS: Array<{ value: ShopProductSort; label: string }> = [
  { value: 'default', label: '综合' },
  { value: 'sales', label: '销量' },
  { value: 'priceAsc', label: '价格↑' },
  { value: 'priceDesc', label: '价格↓' },
];

function parseCategoryId(raw: unknown): number | undefined {
  if (typeof raw !== 'string' || raw === '') return undefined;
  const id = Number(raw);
  return Number.isInteger(id) && id > 0 ? id : undefined;
}

const route = useRoute();
const router = useRouter();

const searchText = ref('');
const keyword = ref('');
const filters = reactive({
  categoryId: parseCategoryId(route.query.categoryId),
  calorie: null as CalorieKey | null,
  price: null as PriceKey | null,
  tags: [] as string[],
  sort: 'default' as ShopProductSort,
});
const panelShow = ref(false);

const { data: categoryTree, isLoading: categoryLoading } = useQuery({
  queryKey: ['shop', 'categories'],
  queryFn: ({ signal }) => getShopCategoryTree(signal),
});
const { data: healthTagOptions } = useQuery({
  queryKey: ['shop', 'healthTags'],
  queryFn: ({ signal }) => getHealthTags(signal),
});

const roots = computed(() => categoryTree.value ?? []);
const activeRoot = computed(() => {
  const id = filters.categoryId;
  if (id === undefined) return undefined;
  const root = roots.value.find((item) => item.id === id);
  if (root) return root;
  return roots.value.find((item) =>
    item.children.some((child) => child.id === id),
  );
});
const subCategories = computed(() => activeRoot.value?.children ?? []);

const params = computed<ShopProductPageParams>(() => {
  const calorie = CALORIE_PRESETS.find((preset) => preset.key === filters.calorie);
  const price = PRICE_PRESETS.find((preset) => preset.key === filters.price);
  const next: ShopProductPageParams = {
    ...calorie?.range,
    ...price?.range,
  };
  if (filters.categoryId !== undefined) next.categoryId = filters.categoryId;
  if (keyword.value !== '') next.keyword = keyword.value;
  if (filters.tags.length > 0) next.healthTags = filters.tags.join(',');
  if (filters.sort !== 'default') next.sort = filters.sort;
  return next;
});

const {
  data: pageData,
  error: productError,
  isLoading: productsLoading,
  isError: productsFailed,
  refetch: refetchProducts,
  hasNextPage,
  fetchNextPage,
} = useInfiniteQuery({
  queryKey: ['shop', 'products', params],
  queryFn: ({ pageParam, signal }) =>
    getShopProductPage({ ...params.value, current: pageParam }, signal),
  initialPageParam: 1,
  getNextPageParam: (lastPage) =>
    lastPage.current < lastPage.pages ? lastPage.current + 1 : undefined,
});

const products = computed(
  () => pageData.value?.pages.flatMap((page) => page.records) ?? [],
);
const total = computed(() => pageData.value?.pages[0]?.total ?? 0);
const errorMessage = computed(() =>
  isApiError(productError.value)
    ? productError.value.message
    : '餐品加载失败，请稍后重试',
);

const listLoading = ref(false);
const listError = ref(false);
const finished = ref(false);

watch(params, () => {
  finished.value = false;
  listError.value = false;
  listLoading.value = false;
});

async function loadMore() {
  try {
    if (hasNextPage.value) await fetchNextPage();
    if (!hasNextPage.value) finished.value = true;
  } catch {
    listError.value = true;
  } finally {
    listLoading.value = false;
  }
}

function retry() {
  finished.value = false;
  listError.value = false;
  void refetchProducts();
}

const panelFilterCount = computed(
  () =>
    (filters.calorie === null ? 0 : 1) +
    (filters.price === null ? 0 : 1) +
    filters.tags.length,
);
const hasConditions = computed(
  () =>
    panelFilterCount.value > 0 ||
    filters.categoryId !== undefined ||
    keyword.value !== '',
);
const emptyDescription = computed(() => {
  if (keyword.value !== '') return '没有找到相关餐品';
  if (hasConditions.value) return '没有符合当前条件的餐品';
  return '暂无可售餐品';
});

function selectCategory(id: number | undefined) {
  filters.categoryId = id;
}

function toggleCalorie(key: CalorieKey) {
  filters.calorie = filters.calorie === key ? null : key;
}

function togglePrice(key: PriceKey) {
  filters.price = filters.price === key ? null : key;
}

function toggleTag(tag: string) {
  const index = filters.tags.indexOf(tag);
  if (index === -1) filters.tags.push(tag);
  else filters.tags.splice(index, 1);
}

function clearPanelFilters() {
  filters.calorie = null;
  filters.price = null;
  filters.tags = [];
}

function applyKeyword() {
  keyword.value = searchText.value.trim();
}

function clearKeyword() {
  searchText.value = '';
  keyword.value = '';
}

function clearConditions() {
  filters.categoryId = undefined;
  filters.calorie = null;
  filters.price = null;
  filters.tags = [];
  searchText.value = '';
  keyword.value = '';
}

// 分类参数与 URL 同步：首页分类入口跳转写入 query，chips 变更回写 query。
watch(
  () => filters.categoryId,
  (id) => {
    if (route.name !== 'catalog') return;
    if (parseCategoryId(route.query.categoryId) === id) return;
    void router.replace({
      name: 'catalog',
      query: { ...route.query, categoryId: id },
    });
  },
);

watch(
  () => (route.name === 'catalog' ? route.query.categoryId : null),
  (raw) => {
    if (raw === null) return;
    const id = parseCategoryId(raw);
    if (filters.categoryId !== id) filters.categoryId = id;
  },
);
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

.catalog-search {
  margin-top: var(--space-4);
}

.category-nav {
  display: grid;
  gap: var(--space-2);
  padding-top: var(--space-3);
}

.category-nav__loading {
  min-height: var(--touch-target);
  color: var(--color-text-secondary);
}

.chip-row {
  display: flex;
  gap: var(--space-2);
  padding-bottom: var(--space-1);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.chip {
  flex: 0 0 auto;
  min-height: var(--touch-target);
  padding: 0 var(--space-3);
  font-size: var(--text-secondary);
  color: var(--color-text);
  white-space: nowrap;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-full);
}

.chip--sub {
  background: var(--color-background-soft);
}

.chip--active {
  font-weight: 700;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-2);
  padding: var(--space-3) 0;
}

.sort-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.sort-tab {
  min-height: var(--touch-target);
  padding: 0 var(--space-1);
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
}

.sort-tab--active {
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-surface);
}

.filter-trigger {
  min-height: var(--touch-target);
  padding: 0 var(--space-3);
  font-size: var(--text-secondary);
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}

.filter-trigger--active {
  font-weight: 700;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.filter-trigger__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  margin-left: var(--space-1);
  font-size: 0.6875rem;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-full);
}

.results {
  min-height: 12rem;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.empty-action {
  margin-top: var(--space-2);
}

.catalog-filter-popup {
  max-height: 75vh;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  max-height: 75vh;
}

.filter-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-4) var(--space-2);

  h2 {
    margin: 0;
    font-size: var(--text-section-title);
    font-weight: 700;
  }
}

.filter-panel__close {
  width: var(--touch-target);
  min-height: var(--touch-target);
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
}

.filter-panel__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 var(--space-4) var(--space-3);
  overflow-y: auto;
}

.filter-group {
  padding-top: var(--space-3);

  h3 {
    display: flex;
    gap: var(--space-2);
    align-items: baseline;
    margin: 0 0 var(--space-2);
    font-size: var(--text-card-title);
    font-weight: 700;
  }
}

.filter-group__hint {
  font-size: var(--text-caption);
  font-weight: 400;
  color: var(--color-text-muted);
}

.filter-group .chip-row {
  flex-wrap: wrap;
  overflow-x: visible;
}

.filter-panel__footer {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-3) var(--space-4);
  padding-bottom: calc(var(--space-3) + env(safe-area-inset-bottom));
  border-top: 1px solid var(--color-border);

  .button-secondary,
  .button-primary {
    flex: 1;
  }
}

.filter-panel__total {
  flex: 1;
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
}

@media (min-width: 560px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 860px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
