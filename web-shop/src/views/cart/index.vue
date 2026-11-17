<template>
  <div class="cart-page page">
    <header class="page-header">
      <div>
        <span>购物车</span>
        <h1>确认餐品与整单热量</h1>
        <p>数量、规格和选项均由服务端重新计算金额。</p>
      </div>
      <strong v-if="cart">{{ cart.totalCalories }} kcal</strong>
    </header>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>购物车加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <van-empty
      v-else-if="cart && cartRows.length === 0"
      description="购物车还是空的"
    >
      <RouterLink class="empty-action" to="/catalog">去选餐</RouterLink>
    </van-empty>

    <template v-else-if="cart">
      <ul class="cart-list" aria-label="购物车商品">
        <li
          v-for="item in cartRows"
          :key="item.id"
          class="cart-card surface-card"
          :class="{ 'cart-card--unavailable': !item.available }"
        >
          <div class="cart-card__main">
            <van-checkbox
              :model-value="item.checked"
              :disabled="!item.available"
              shape="square"
              aria-label="选择商品"
              @update:model-value="(checked) => toggleChecked(item, checked)"
            />
            <img :src="item.cover" :alt="item.productName" />
            <div class="cart-card__content">
              <div class="cart-card__title">
                <h2>{{ item.productName }}</h2>
                <van-tag v-if="!item.available" type="danger">不可购买</van-tag>
              </div>
              <p v-if="item.specName || item.optionsDesc">
                {{ [item.specName, item.optionsDesc].filter(Boolean).join(' · ') }}
              </p>
              <div class="cart-card__meta">
                <b>{{ formatPrice(item.unitPrice) }}</b>
                <span>{{ item.subtotalCalories }} kcal</span>
              </div>
            </div>
          </div>

          <div class="cart-card__actions">
            <button
              type="button"
              class="text-action"
              :disabled="!item.available"
              @click="openEditor(item)"
            >
              改规格选项
            </button>
            <van-stepper
              :model-value="item.quantity"
              min="1"
              max="99"
              :disabled="!item.available"
              :disabled-input="!item.available"
              @update:model-value="(quantity) => changeQuantity(item, quantity)"
            />
            <button
              type="button"
              class="icon-action"
              :aria-label="`删除${item.productName}`"
              @click="confirmDelete(item)"
            >
              删除
            </button>
          </div>
        </li>
      </ul>

      <section class="cart-summary surface-card" aria-label="购物车合计">
        <div>
          <span>已选 {{ cart.checkedCount }} 项</span>
          <strong>{{ formatPrice(cart.goodsAmount) }}</strong>
        </div>
        <div>
          <span>整单热量</span>
          <strong>{{ cart.totalCalories }} kcal</strong>
        </div>
        <van-button
          type="primary"
          size="large"
          :disabled="cart.checkedCount === 0"
          @click="router.push('/checkout')"
        >
          去结算
        </van-button>
      </section>
    </template>

    <van-popup
      v-model:show="editorVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      class="option-editor"
    >
      <div class="option-editor__header">
        <div>
          <span>修改规格</span>
          <h2>{{ selectedLine?.productName }}</h2>
        </div>
        <b v-if="editorProduct">{{ formatPrice(editorUnitPrice) }}</b>
      </div>

      <div v-if="editorLoading" class="state-panel">
        <van-loading vertical>商品配置加载中…</van-loading>
      </div>
      <div v-else-if="editorError" class="state-panel" role="alert">
        <p>{{ editorErrorText }}</p>
      </div>
      <template v-else-if="editorProduct">
        <section v-if="editorSpecs.length > 0" class="editor-block">
          <h3>规格</h3>
          <van-radio-group v-model="editorSpecId" class="editor-options">
            <van-radio
              v-for="spec in editorSpecs"
              :key="spec.id"
              :name="spec.id"
              class="editor-option"
            >
              <span>{{ spec.name }}</span>
              <span>
                {{ formatPrice(editorProduct.price + spec.priceDelta) }}
              </span>
            </van-radio>
          </van-radio-group>
        </section>

        <section
          v-for="group in editorGroups"
          :key="group.id"
          class="editor-block"
        >
          <h3>
            {{ group.name }}
            <van-tag v-if="group.required === 1" plain>必选</van-tag>
            <van-tag v-if="group.type === 2" plain>可多选</van-tag>
          </h3>
          <p v-if="editorMissing.includes(group.name)" class="editor-error">
            请选择{{ group.name }}
          </p>
          <van-radio-group
            v-if="group.type === 1"
            v-model="editorRadio[group.id]"
            class="editor-options"
          >
            <van-radio
              v-for="item in group.items"
              :key="item.id"
              :name="item.id"
              class="editor-option"
            >
              <span>{{ item.name }}</span>
              <span>
                <template v-if="item.calories > 0">+{{ item.calories }} kcal </template>
                {{ item.priceDelta === 0 ? '' : formatDelta(item.priceDelta) }}
              </span>
            </van-radio>
          </van-radio-group>
          <van-checkbox-group
            v-else
            v-model="editorCheckbox[group.id]"
            class="editor-options"
          >
            <van-checkbox
              v-for="item in group.items"
              :key="item.id"
              :name="item.id"
              shape="square"
              class="editor-option"
            >
              <span>{{ item.name }}</span>
              <span>
                <template v-if="item.calories > 0">+{{ item.calories }} kcal </template>
                {{ item.priceDelta === 0 ? '' : formatDelta(item.priceDelta) }}
              </span>
            </van-checkbox>
          </van-checkbox-group>
        </section>

        <div class="option-editor__footer">
          <van-button
            block
            type="primary"
            size="large"
            :loading="editorSaving"
            :disabled="editorMissing.length > 0 || !editorSpecId"
            @click="saveEditor"
          >
            保存修改
          </van-button>
        </div>
      </template>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { useMutation, useQuery } from '@tanstack/vue-query';
import {
  cartQueryKey,
  deleteCartItem,
  getCart,
  updateCartItem,
} from '@/api/modules/trade';
import type { CartLine } from '@/api/modules/trade';
import { getShopProductDetail } from '@/api/modules/product';
import type { ProductOptionItem } from '@/api/modules/product';
import { isApiError } from '@/types/api/common';
import { formatDelta, formatPrice } from '@/utils/product';

defineOptions({ name: 'CartPage' });

interface EditorGroup {
  id: number;
  name: string;
  type: number;
  required: number;
  items: ProductOptionItem[];
}

/** 购物车行项必定带购物车记录 ID，立即购买行项才会缺失该字段。 */
type CartRow = CartLine & { id: number };

const router = useRouter();
const editorVisible = ref(false);
const editorSaving = ref(false);
const selectedLineId = ref<number | null>(null);
const editorSpecId = ref<number | undefined>(undefined);
const editorRadio = ref<Record<number, number | undefined>>({});
const editorCheckbox = ref<Record<number, number[]>>({});

const {
  data: cart,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: cartQueryKey,
  queryFn: ({ signal }) => getCart(signal),
});

/** 购物车返回的行项均来自购物车表，ID 必定存在。 */
const cartRows = computed<CartRow[]>(() =>
  (cart.value?.items ?? []).filter(
    (item): item is CartRow => typeof item.id === 'number',
  ),
);

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '购物车加载失败',
);

const selectedLine = computed(() =>
  cartRows.value.find((item) => item.id === selectedLineId.value),
);
const editorProductId = computed(() => selectedLine.value?.productId);

const {
  data: editorProduct,
  isLoading: editorLoading,
  isError: editorFailed,
  error: editorQueryError,
} = useQuery({
  queryKey: ['shop', 'product', editorProductId],
  queryFn: ({ signal }) =>
    getShopProductDetail(Number(editorProductId.value), signal),
  enabled: computed(
    () => editorVisible.value && typeof editorProductId.value === 'number',
  ),
});

const editorErrorText = computed(() =>
  isApiError(editorQueryError.value)
    ? editorQueryError.value.message
    : '商品配置加载失败',
);
const editorError = computed(() => editorFailed.value);
const editorSpecs = computed(() =>
  (editorProduct.value?.specs ?? []).filter((spec) => spec.status === 1),
);
const editorGroups = computed<EditorGroup[]>(() =>
  (editorProduct.value?.options ?? [])
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

const editorMissing = computed(() =>
  editorGroups.value
    .filter((group) => {
      if (group.required !== 1) return false;
      if (group.type === 1) return editorRadio.value[group.id] === undefined;
      return (editorCheckbox.value[group.id] ?? []).length === 0;
    })
    .map((group) => group.name),
);

const editorUnitPrice = computed(() => {
  const product = editorProduct.value;
  if (!product) return 0;
  const spec = editorSpecs.value.find(
    (item) => item.id === editorSpecId.value,
  );
  let price = product.price + (spec?.priceDelta ?? 0);
  for (const group of editorGroups.value) {
    let ids: number[] = editorCheckbox.value[group.id] ?? [];
    if (group.type === 1) {
      const selectedId = editorRadio.value[group.id];
      ids = selectedId === undefined ? [] : [selectedId];
    }
    price += group.items
      .filter((item) => ids.includes(item.id))
      .reduce((total, item) => total + item.priceDelta, 0);
  }
  return price;
});

watch(
  [editorProduct, selectedLine],
  ([product, line]) => {
    if (!product || !line) return;
    editorSpecId.value = line.specId;
    const selectedIds = new Set(line.options.map((option) => option.itemId));
    const radio: Record<number, number | undefined> = {};
    const checkbox: Record<number, number[]> = {};
    for (const group of product.options) {
      const activeItems = group.items.filter((item) => item.status === 1);
      const selected = activeItems.filter((item) => selectedIds.has(item.id));
      if (group.type === 2) {
        checkbox[group.id] = selected.map((item) => item.id);
      } else {
        radio[group.id] = selected[0]?.id;
      }
    }
    editorRadio.value = radio;
    editorCheckbox.value = checkbox;
  },
  { immediate: true },
);

// 写接口（addCartItem/updateCartItem/deleteCartItem）在 trade 模块内统一失效 cartQueryKey，
// 页面层无需重复 invalidate。
const cartMutation = useMutation({
  mutationFn: ({
    id,
    body,
  }: {
    id: number;
    body: Parameters<typeof updateCartItem>[1];
  }) => updateCartItem(id, body),
  onError: (error) => showToast(messageOf(error, '购物车更新失败')),
});

function messageOf(error: unknown, fallback: string) {
  return isApiError(error) ? error.message : fallback;
}

function toggleChecked(item: CartRow, checked: boolean) {
  cartMutation.mutate({ id: item.id, body: { checked } });
}

function changeQuantity(item: CartRow, quantity: number | string) {
  const value = Number(quantity);
  if (!Number.isInteger(value) || value < 1 || value > 99) return;
  cartMutation.mutate({ id: item.id, body: { quantity: value } });
}

function openEditor(item: CartRow) {
  selectedLineId.value = item.id;
  editorVisible.value = true;
}

async function saveEditor() {
  const item = selectedLine.value;
  if (!item || !editorSpecId.value || editorMissing.value.length > 0) return;
  const optionIds: number[] = [];
  for (const group of editorGroups.value) {
    if (group.type === 1) {
      const id = editorRadio.value[group.id];
      if (id !== undefined) optionIds.push(id);
    } else {
      optionIds.push(...(editorCheckbox.value[group.id] ?? []));
    }
  }
  editorSaving.value = true;
  try {
    await updateCartItem(item.id, {
      specId: editorSpecId.value,
      optionItemIds: optionIds,
    });
    editorVisible.value = false;
    showToast({ message: '规格已更新', icon: 'success' });
  } catch (error) {
    showToast(messageOf(error, '规格更新失败'));
  } finally {
    editorSaving.value = false;
  }
}

function confirmDelete(item: CartRow) {
  void showConfirmDialog({
    title: '删除商品',
    message: `从购物车移除“${item.productName}”？`,
    confirmButtonColor: 'var(--color-danger)',
  }).then(async () => {
    try {
      await deleteCartItem(item.id);
      showToast({ message: '已删除', icon: 'success' });
    } catch (error) {
      showToast(messageOf(error, '删除失败'));
    }
  });
}
</script>

<style scoped lang="scss">
.cart-page {
  display: grid;
  gap: var(--space-4);
  padding-top: calc(var(--space-5) + env(safe-area-inset-top));
}

.page-header strong {
  align-self: center;
  color: var(--color-primary);
  white-space: nowrap;
}

.cart-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;
}

.cart-card {
  display: grid;
  gap: var(--space-3);

  &--unavailable {
    background: var(--color-danger-soft);
    opacity: 0.78;
  }
}

.cart-card__main {
  display: grid;
  grid-template-columns: auto 4.75rem 1fr;
  gap: var(--space-3);
  align-items: center;
}

.cart-card__main img {
  width: 4.75rem;
  height: 4.75rem;
  object-fit: cover;
  background: var(--color-background-soft);
  border-radius: var(--radius-md);
}

.cart-card__content {
  min-width: 0;
}

.cart-card__title {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: var(--text-card-title);
    white-space: nowrap;
  }
}

.cart-card__content > p {
  margin: var(--space-1) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.cart-card__meta {
  display: flex;
  gap: var(--space-3);
  align-items: baseline;
  justify-content: space-between;

  b {
    color: var(--color-primary);
  }

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
}

.cart-card__actions {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: var(--space-3);
  align-items: center;
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.text-action,
.icon-action {
  min-height: var(--touch-target);
  padding: 0;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
}

.text-action {
  justify-self: start;
  font-weight: 650;
  color: var(--color-primary);
}

.icon-action {
  font-size: var(--text-caption);
}

.cart-summary {
  display: grid;
  gap: var(--space-3);
  padding-bottom: calc(var(--space-4) + env(safe-area-inset-bottom));

  > div {
    display: flex;
    align-items: center;
    justify-content: space-between;

    span {
      color: var(--color-text-secondary);
    }

    strong {
      font-size: var(--text-section-title);
    }
  }
}

.empty-action {
  display: inline-flex;
  align-items: center;
  min-height: var(--touch-target);
  padding: 0 var(--space-4);
  font-weight: 700;
  color: var(--color-primary-contrast);
  text-decoration: none;
  background: var(--color-primary);
  border-radius: var(--radius-md);
}

.option-editor {
  max-height: min(88dvh, 48rem);
  overflow: hidden auto;
}

.option-editor__header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  h2 {
    margin: var(--space-1) 0 0;
    font-size: var(--text-card-title);
  }

  b {
    font-size: var(--text-section-title);
    color: var(--color-primary);
  }
}

.editor-block {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);

  h3 {
    display: flex;
    gap: var(--space-2);
    align-items: center;
    margin: 0 0 var(--space-2);
    font-size: var(--text-body);
  }
}

.editor-options {
  display: grid;
}

.editor-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: var(--touch-target);

  :deep(.van-radio__label),
  :deep(.van-checkbox__label) {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
    margin-left: var(--space-3);
  }
}

.editor-error {
  margin: 0 0 var(--space-2);
  font-size: var(--text-caption);
  color: var(--color-danger);
}

.option-editor__footer {
  position: sticky;
  bottom: 0;
  padding: var(--space-4);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}
</style>
