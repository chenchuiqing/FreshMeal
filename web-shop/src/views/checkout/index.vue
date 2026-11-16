<template>
  <div class="checkout-page page">
    <header class="page-header">
      <div>
        <span>结算</span>
        <h1>确认配送与最终金额</h1>
        <p>提交前由服务端重新校验商品状态、库存和运费规则。</p>
      </div>
    </header>

    <section class="checkout-section surface-card">
      <div class="section-title">
        <h2>配送方式</h2>
      </div>
      <van-radio-group v-model="deliveryType" class="delivery-options">
        <van-radio :name="1" class="delivery-option">
          <span>外卖配送</span>
          <small>按地址匹配区域与里程计费</small>
        </van-radio>
        <van-radio :name="2" class="delivery-option">
          <span>到店自提</span>
          <small>免配送费</small>
        </van-radio>
      </van-radio-group>
    </section>

    <section class="checkout-section surface-card">
      <div class="section-title">
        <h2>收货地址</h2>
        <button type="button" class="text-button" @click="addressPopup = true">
          切换
        </button>
      </div>
      <button
        v-if="currentAddress"
        type="button"
        class="address-button"
        @click="addressPopup = true"
      >
        <span>
          <strong>{{ currentAddress.receiver }}</strong>
          {{ currentAddress.phone }}
          <van-tag v-if="currentAddress.isDefault" type="primary">默认</van-tag>
        </span>
        <small>{{ currentAddress.fullAddress }}</small>
      </button>
      <div v-else class="inline-state">
        <p>{{ addressesError ? addressesErrorText : '请先添加收货地址' }}</p>
        <RouterLink class="button-secondary" to="/address">管理地址</RouterLink>
      </div>
    </section>

    <section class="checkout-section surface-card">
      <div class="section-title">
        <h2>优惠券</h2>
        <RouterLink class="text-button" to="/member/coupons">
          管理优惠券
        </RouterLink>
      </div>
      <div v-if="couponsLoading" class="state-panel">
        <van-loading vertical>优惠券加载中…</van-loading>
      </div>
      <div v-else-if="couponsFailed" class="state-panel" role="alert">
        <p>{{ couponsErrorText }}</p>
        <van-button plain type="primary" @click="refetchCoupons">
          重新加载
        </van-button>
      </div>
      <van-radio-group
        v-else
        v-model="selectedUserCouponId"
        class="coupon-options"
      >
        <van-radio :name="null" class="coupon-option">
          <span>
            <strong>不使用优惠券</strong>
            <small>按原价结算</small>
          </span>
        </van-radio>
        <van-radio
          v-for="coupon in usableCoupons"
          :key="coupon.id"
          :name="coupon.id"
          class="coupon-option"
        >
          <span>
            <strong>{{ coupon.name }}</strong>
            <small>
              满 {{ formatPrice(coupon.threshold) }} 减
              {{ coupon.type === 1 ? formatPrice(coupon.amount) : `${Math.round(Number(coupon.amount) * 10)}折` }}
            </small>
          </span>
        </van-radio>
      </van-radio-group>
    </section>

    <section v-if="deliveryType === 1" class="checkout-section surface-card">
      <div class="section-title">
        <h2>预约时段</h2>
        <span>{{ slots?.length ?? 0 }} 个可选</span>
      </div>
      <div v-if="slotsLoading" class="state-panel">
        <van-loading vertical>正在查询可预约时段…</van-loading>
      </div>
      <div v-else-if="slotsFailed" class="state-panel" role="alert">
        <p>{{ slotsErrorText }}</p>
        <van-button plain type="primary" @click="refetchSlots">
          重新查询
        </van-button>
      </div>
      <div v-else-if="!slots?.length" class="inline-state">
        <p>当前没有可预约的配送时段，请稍后再试。</p>
      </div>
      <div v-else class="slot-list">
        <button
          v-for="slot in slots"
          :key="slotKey(slot)"
          type="button"
          class="slot-option"
          :class="{ 'slot-option--selected': selectedSlotKey === slotKey(slot) }"
          @click="selectedSlotKey = slotKey(slot)"
        >
          <strong>{{ slot.name }}</strong>
          <span>{{ formatDate(slot.date) }}</span>
          <small v-if="slot.remainingCapacity != null">
            余 {{ slot.remainingCapacity }} 单
          </small>
          <small v-else>仍有名额</small>
        </button>
      </div>
    </section>

    <section class="checkout-section surface-card">
      <div class="section-title">
        <h2>餐品清单</h2>
        <RouterLink
          v-if="isDirectMode"
          class="text-button"
          :to="{ name: 'productDetail', params: { id: directProductId } }"
        >
          返回修改
        </RouterLink>
        <RouterLink v-else class="text-button" to="/cart">返回修改</RouterLink>
      </div>
      <div v-if="cartLoading || settleLoading" class="state-panel">
        <van-loading vertical>正在计算最终金额…</van-loading>
      </div>
      <div v-else-if="settleError" class="state-panel" role="alert">
        <p>{{ settleErrorText }}</p>
        <van-button plain type="primary" @click="refetchSettle">重新计算</van-button>
      </div>
      <template v-else-if="settle">
        <ul class="item-list">
          <li v-for="item in settle.items" :key="item.id ?? item.productId">
            <img :src="item.cover" :alt="item.productName" />
            <div>
              <strong>{{ item.productName }}</strong>
              <p>{{ [item.specName, item.optionsDesc].filter(Boolean).join(' · ') }}</p>
              <span>× {{ item.quantity }}</span>
            </div>
            <b>{{ formatPrice(item.subtotal) }}</b>
          </li>
        </ul>

        <dl class="amount-list">
          <div>
            <dt>商品金额</dt>
            <dd>{{ formatPrice(settle.goodsAmount) }}</dd>
          </div>
          <div>
            <dt>
              配送费
              <small v-if="settle.freight.areaName">
                {{ settle.freight.areaName }} · {{ settle.freight.distance }} km
              </small>
              <van-tag v-if="settle.freight.freeShipping" type="success">已免运费</van-tag>
            </dt>
            <dd>{{ formatPrice(settle.freightAmount) }}</dd>
          </div>
          <div>
            <dt>优惠金额</dt>
            <dd>-{{ formatPrice(settle.discountAmount) }}</dd>
          </div>
          <div>
            <dt>整单热量</dt>
            <dd>{{ settle.totalCalories }} kcal</dd>
          </div>
          <div class="amount-list__total">
            <dt>实付金额</dt>
            <dd>{{ formatPrice(settle.payAmount) }}</dd>
          </div>
        </dl>
      </template>
    </section>

    <section class="checkout-section surface-card">
      <label class="field-label" for="order-remark">订单备注</label>
      <van-field
        id="order-remark"
        v-model="remark"
        rows="2"
        autosize
        type="textarea"
        maxlength="200"
        show-word-limit
        placeholder="口味、放置位置等（选填）"
      />
    </section>

    <div class="checkout-submit">
      <div>
        <span>实付</span>
        <strong>{{ settle ? formatPrice(settle.payAmount) : '—' }}</strong>
      </div>
      <van-button
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="
          !settle ||
          submitting ||
          (deliveryType === 1 && !selectedSlot)
        "
        loading-text="提交中…"
        @click="submitOrder"
      >
        提交订单
      </van-button>
    </div>

    <van-popup v-model:show="addressPopup" position="bottom" round safe-area-inset-bottom>
      <div class="address-picker">
        <h2>选择收货地址</h2>
        <van-radio-group
          :model-value="selectedAddressId"
          @update:model-value="selectAddress"
        >
          <van-radio
            v-for="address in addresses"
            :key="address.id"
            :name="address.id"
            class="address-option"
          >
            <span>
              <strong>{{ address.receiver }} {{ address.phone }}</strong>
              <small>{{ address.fullAddress }}</small>
            </span>
          </van-radio>
        </van-radio-group>
        <RouterLink class="button-secondary" to="/address">管理收货地址</RouterLink>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { useQuery, useQueryClient } from '@tanstack/vue-query';
import {
  createOrder,
  getAvailableDeliverySlots,
  getCart,
  listAddresses,
  settleCart,
} from '@/api/modules/trade';
import { isApiError } from '@/types/api/common';
import { listUserCoupons } from '@/api/modules/marketing';
import type { UserCoupon } from '@/api/modules/marketing';
import { useCheckoutStore } from '@/store/modules/checkout';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'CheckoutPage' });

const router = useRouter();
const route = useRoute();
const queryClient = useQueryClient();
const checkoutStore = useCheckoutStore();
const deliveryType = ref<1 | 2>(1);
const selectedAddressId = ref<number | null>(null);
const addressPopup = ref(false);
const remark = ref('');
const submitting = ref(false);
const selectedSlotKey = ref<string | null>(null);
const selectedUserCouponId = ref<number | null>(null);

/** 立即购买模式：结算只包含草稿商品，不读取购物车勾选项。 */
const isDirectMode = computed(
  () =>
    route.query.mode === 'direct' &&
    checkoutStore.directDraft !== null,
);
const directProductId = computed(
  () => checkoutStore.directDraft?.productId ?? 0,
);

const {
  data: userCoupons,
  isLoading: couponsLoading,
  isError: couponsFailed,
  error: couponsQueryError,
  refetch: refetchCoupons,
} = useQuery({
  queryKey: ['shop', 'coupons', 'mine'],
  queryFn: ({ signal }) => listUserCoupons(signal),
});

const couponsErrorText = computed(() =>
  isApiError(couponsQueryError.value)
    ? couponsQueryError.value.message
    : '优惠券加载失败',
);

const {
  data: addresses,
  isError: addressesFailed,
  error: addressesQueryError,
} = useQuery({
  queryKey: ['shop', 'addresses'],
  queryFn: ({ signal }) => listAddresses(signal),
});

const addressesError = computed(() => addressesFailed.value);
const addressesErrorText = computed(() =>
  isApiError(addressesQueryError.value)
    ? addressesQueryError.value.message
    : '地址加载失败',
);

watch(
  addresses,
  (items) => {
    if (!items?.length) return;
    if (
      selectedAddressId.value === null ||
      !items.some((item) => item.id === selectedAddressId.value)
    ) {
      const nextAddressId =
        items.find((item) => item.isDefault)?.id ?? items[0]?.id;
      if (typeof nextAddressId === 'number') {
        selectedAddressId.value = nextAddressId;
      }
    }
  },
  { immediate: true },
);

const currentAddress = computed(() =>
  addresses.value?.find((address) => address.id === selectedAddressId.value),
);

const {
  data: slots,
  isLoading: slotsLoading,
  isError: slotsFailed,
  error: slotsQueryError,
  refetch: refetchSlots,
} = useQuery({
  queryKey: ['shop', 'delivery-slots'],
  queryFn: ({ signal }) => getAvailableDeliverySlots(signal),
  enabled: computed(() => deliveryType.value === 1),
});

const slotsErrorText = computed(() =>
  isApiError(slotsQueryError.value)
    ? slotsQueryError.value.message
    : '配送时段加载失败',
);

const selectedSlot = computed(() =>
  slots.value?.find(
    (slot) => selectedSlotKey.value === `${slot.id}-${slot.date}`,
  ),
);

watch(
  slots,
  (items) => {
    if (deliveryType.value !== 1 || !items?.length) return;
    const exists = items.some(
      (slot) => selectedSlotKey.value === `${slot.id}-${slot.date}`,
    );
    const [firstSlot] = items;
    if (!exists && firstSlot) {
      selectedSlotKey.value = `${firstSlot.id}-${firstSlot.date}`;
    }
  },
  { immediate: true },
);

watch(deliveryType, (value) => {
  if (value === 2) {
    selectedSlotKey.value = null;
    return;
  }
  const [firstSlot] = slots.value ?? [];
  if (firstSlot && selectedSlotKey.value === null) {
    selectedSlotKey.value = `${firstSlot.id}-${firstSlot.date}`;
  }
});

const {
  data: cart,
  isLoading: cartLoading,
  isError: cartFailed,
  error: cartQueryError,
} = useQuery({
  queryKey: ['shop', 'cart'],
  queryFn: ({ signal }) => getCart(signal),
  enabled: computed(() => !isDirectMode.value),
});

const checkedCartIds = computed(() =>
  (cart.value?.items ?? [])
    .filter((item) => item.checked && item.available)
    .map((item) => item.id)
    .filter((id): id is number => typeof id === 'number'),
);

const usableCoupons = computed(() =>
  (userCoupons.value ?? []).filter((coupon: UserCoupon) => {
    if (coupon.status !== 0) return false;
    const expire = new Date(coupon.expireTime).getTime();
    return Number.isNaN(expire) || expire > Date.now();
  }),
);

const directItems = computed(() => {
  const draft = checkoutStore.directDraft;
  if (!isDirectMode.value || !draft) return undefined;
  return [
    {
      productId: draft.productId,
      specId: draft.specId,
      optionItemIds: draft.optionItemIds,
      quantity: draft.quantity,
    },
  ];
});

const settleParams = computed(() =>
  currentAddress.value
    ? {
        addressId: currentAddress.value.id,
        deliveryType: deliveryType.value,
        cartItemIds: isDirectMode.value ? undefined : checkedCartIds.value,
        directItems: directItems.value,
        userCouponId: selectedUserCouponId.value ?? undefined,
      }
    : undefined,
);

const {
  data: settle,
  isLoading: settleLoading,
  isError: settleFailed,
  error: settleQueryError,
  refetch: refetchSettle,
} = useQuery({
  queryKey: [
    'shop',
    'settle',
    selectedAddressId,
    deliveryType,
    checkedCartIds,
    selectedUserCouponId,
    directItems,
  ],
  queryFn: ({ signal }) => {
    const params = settleParams.value;
    if (!params) throw new Error('结算参数尚未就绪');
    return settleCart(params, signal);
  },
  enabled: computed(
    () =>
      settleParams.value !== undefined &&
      (isDirectMode.value
        ? directItems.value !== undefined
        : checkedCartIds.value.length > 0) &&
      !cartLoading.value &&
      !cartFailed.value,
  ),
});

const settleError = computed(
  () =>
    settleFailed.value ||
    cartFailed.value ||
    (!isDirectMode.value &&
      cart.value !== undefined &&
      checkedCartIds.value.length === 0),
);
const settleErrorText = computed(() => {
  if (cartFailed.value) {
    return isApiError(cartQueryError.value)
      ? cartQueryError.value.message
      : '购物车加载失败';
  }
  if (
    !isDirectMode.value &&
    cart.value !== undefined &&
    checkedCartIds.value.length === 0
  ) {
    return '请先在购物车选择可购买商品';
  }
  return isApiError(settleQueryError.value)
    ? settleQueryError.value.message
    : '结算计算失败';
});

function selectAddress(value: unknown) {
  if (typeof value === 'number') selectedAddressId.value = value;
  addressPopup.value = false;
}

async function submitOrder() {
  const address = currentAddress.value;
  if (
    !address ||
    !settle.value ||
    submitting.value ||
    (deliveryType.value === 1 && !selectedSlot.value)
  ) {
    if (deliveryType.value === 1 && !selectedSlot.value) {
      showToast('请选择可用的配送日期与时段');
    }
    return;
  }
  submitting.value = true;
  try {
    const order = await createOrder({
      addressId: address.id,
      deliveryType: deliveryType.value,
      cartItemIds: isDirectMode.value ? undefined : checkedCartIds.value,
      directItems: directItems.value,
      deliverySlotId: selectedSlot.value?.id,
      deliverySlotDate: selectedSlot.value?.date,
      remark: remark.value.trim() || undefined,
      userCouponId: selectedUserCouponId.value ?? undefined,
    });
    checkoutStore.clearDirectDraft();
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['shop', 'cart'] }),
      queryClient.invalidateQueries({ queryKey: ['shop', 'orders'] }),
    ]);
    await router.replace(`/pay/${order.orderNo}`);
  } catch (error) {
    showToast(
      isApiError(error) ? error.message : '提交订单失败，请稍后重试',
    );
    if (isApiError(error) && error.status === 409) {
      await Promise.all([
        refetchSettle(),
        queryClient.invalidateQueries({ queryKey: ['shop', 'cart'] }),
      ]);
    }
  } finally {
    submitting.value = false;
  }
}

function slotKey(slot: { id: number; date: string }) {
  return `${slot.id}-${slot.date}`;
}

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    weekday: 'short',
  }).format(date);
}
</script>

<style scoped lang="scss">
.checkout-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
  padding-bottom: calc(var(--space-10) + env(safe-area-inset-bottom));
}

.checkout-section {
  display: grid;
  gap: var(--space-3);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: var(--text-card-title);
  }
}

.text-button {
  min-height: var(--touch-target);
  padding: 0;
  font-weight: 650;
  color: var(--color-primary);
  text-decoration: none;
  background: transparent;
  border: 0;
}

.delivery-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.delivery-option {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  align-items: flex-start;
  min-height: 5rem;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  :deep(.van-radio__icon) {
    margin-left: auto;
  }

  :deep(.van-radio__label) {
    display: grid;
    flex: 1;
    gap: var(--space-1);
    margin-left: 0;
  }

  span {
    font-weight: 700;
  }

  small {
    color: var(--color-text-secondary);
  }
}

.address-button {
  display: grid;
  gap: var(--space-1);
  width: 100%;
  padding: var(--space-3);
  text-align: left;
  background: var(--color-background-soft);
  border: 0;
  border-radius: var(--radius-md);

  span {
    display: flex;
    gap: var(--space-2);
    align-items: center;
  }

  small {
    line-height: var(--leading-body);
    color: var(--color-text-secondary);
  }
}

.coupon-options {
  display: grid;
  gap: var(--space-2);
}

.coupon-option {
  display: flex;
  align-items: center;
  min-height: var(--touch-target);
  padding: var(--space-3);
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  :deep(.van-radio__label) {
    display: grid;
    flex: 1;
    gap: var(--space-1);
    margin-left: var(--space-3);
  }

  span {
    display: grid;
    gap: var(--space-1);
  }

  strong {
    font-weight: 700;
  }

  small {
    color: var(--color-text-secondary);
  }
}

.inline-state {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;

  p {
    margin: 0;
    color: var(--color-text-secondary);
  }
}

.item-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    display: grid;
    grid-template-columns: 3.5rem 1fr auto;
    gap: var(--space-3);
    align-items: center;
  }

  img {
    width: 3.5rem;
    height: 3.5rem;
    object-fit: cover;
    background: var(--color-background-soft);
    border-radius: var(--radius-md);
  }

  strong {
    display: block;
  }

  p {
    margin: var(--space-1) 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
}

.amount-list {
  display: grid;
  gap: var(--space-2);
  padding-top: var(--space-3);
  margin: 0;
  border-top: 1px solid var(--color-border);

  > div {
    display: flex;
    gap: var(--space-4);
    align-items: center;
    justify-content: space-between;
  }

  dt {
    display: flex;
    gap: var(--space-2);
    align-items: center;
    color: var(--color-text-secondary);
  }

  dt small {
    font-size: var(--text-caption);
  }

  dd {
    margin: 0;
    font-weight: 650;
  }

  &__total {
    padding-top: var(--space-3);
    border-top: 1px solid var(--color-border);

    dt,
    dd {
      font-weight: 750;
      color: var(--color-text);
    }

    dd {
      font-size: var(--text-section-title);
      color: var(--color-primary);
    }
  }
}

.field-label {
  font-weight: 650;
}

.checkout-submit {
  position: sticky;
  bottom: var(--space-2);
  z-index: 5;
  display: grid;
  grid-template-columns: minmax(6rem, auto) 1fr;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-3);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);

  > div {
    display: grid;

    span {
      font-size: var(--text-caption);
      color: var(--color-text-secondary);
    }

    strong {
      font-size: var(--text-section-title);
      color: var(--color-primary);
    }
  }
}

.address-picker {
  display: grid;
  gap: var(--space-4);
  max-height: 75dvh;
  padding: var(--space-5) var(--space-4)
    calc(var(--space-5) + env(safe-area-inset-bottom));
  overflow: auto;

  h2 {
    margin: 0;
    font-size: var(--text-section-title);
    text-align: center;
  }
}

.address-option {
  align-items: flex-start;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  :deep(.van-radio__label) {
    display: grid;
    gap: var(--space-1);
    min-width: 0;
    margin-left: var(--space-3);
  }

  small {
    color: var(--color-text-secondary);
  }
}

.slot-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.slot-option {
  display: grid;
  gap: var(--space-1);
  min-height: 5.5rem;
  padding: var(--space-3);
  text-align: left;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  strong {
    font-size: var(--text-body);
  }

  span,
  small {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
}

.slot-option--selected {
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
  box-shadow: inset 0 0 0 1px var(--color-primary);

  strong,
  span,
  small {
    color: var(--color-primary);
  }
}
</style>
