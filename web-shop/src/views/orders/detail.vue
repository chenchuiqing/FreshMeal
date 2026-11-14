<template>
  <div class="order-detail-page page">
    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>订单详情加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <template v-else-if="order">
      <section class="status-hero" :class="`status-hero--${order.status}`">
        <div>
          <span>订单状态</span>
          <h1>{{ order.statusText }}</h1>
          <p v-if="order.cancelReason">{{ order.cancelReason }}</p>
          <p v-else>{{ statusDescription }}</p>
        </div>
        <van-icon v-if="order.status === 6" name="checked" />
        <van-icon v-else-if="order.status === 9" name="close" />
        <van-icon v-else name="clock-o" />
      </section>

      <section class="detail-section surface-card">
        <div class="section-title">
          <h2>收货信息</h2>
          <span>{{ order.deliveryType === 1 ? '外卖配送' : '到店自提' }}</span>
        </div>
        <p>
          <strong>{{ order.receiver }}</strong>
          {{ order.receiverPhone }}
        </p>
        <p>{{ order.receiverAddress }}</p>
        <small>订单号 {{ order.orderNo }}</small>
      </section>

      <section class="detail-section surface-card">
        <div class="section-title">
          <h2>商品明细</h2>
          <span>{{ order.items.reduce((sum, item) => sum + item.subtotalCalories, 0) }} kcal</span>
        </div>
        <ul class="item-list">
          <li v-for="(item, index) in order.items" :key="`${item.productId}-${index}`">
            <img :src="item.productCover" :alt="item.productName" />
            <div>
              <strong>{{ item.productName }}</strong>
              <p>{{ [item.specName, item.optionsDesc].filter(Boolean).join(' · ') }}</p>
              <span>{{ formatPrice(item.price) }} × {{ item.quantity }}</span>
            </div>
            <b>{{ formatPrice(item.subtotal) }}</b>
          </li>
        </ul>

        <dl class="amounts">
          <div><dt>商品金额</dt><dd>{{ formatPrice(order.goodsAmount) }}</dd></div>
          <div><dt>配送费</dt><dd>{{ formatPrice(order.freightAmount) }}</dd></div>
          <div><dt>优惠金额</dt><dd>-{{ formatPrice(order.discountAmount) }}</dd></div>
          <div class="amounts__total">
            <dt>实付金额</dt>
            <dd>{{ formatPrice(order.payAmount) }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-section surface-card">
        <div class="section-title">
          <h2>订单进度</h2>
          <span>{{ order.timeline.length }} 个节点</span>
        </div>
        <ol class="timeline">
          <li
            v-for="(node, index) in [...order.timeline].reverse()"
            :key="`${node.status}-${node.time}`"
            :class="{ current: index === 0 }"
          >
            <span class="timeline__dot" />
            <div>
              <strong>{{ node.statusText }}</strong>
              <p v-if="node.remark">{{ node.remark }}</p>
              <small>
                {{ node.operatorName }} · {{ formatDateTime(node.time) }}
              </small>
            </div>
          </li>
        </ol>
      </section>

      <section
        v-if="deliveryTrace && deliveryTrace.length"
        class="detail-section surface-card"
      >
        <div class="section-title">
          <h2>配送进度</h2>
          <span>{{ deliveryTrace.length }} 个节点</span>
        </div>
        <ol class="timeline">
          <li
            v-for="(node, index) in [...deliveryTrace].reverse()"
            :key="`${node.status}-${node.time}`"
            :class="{ current: index === 0 }"
          >
            <span class="timeline__dot" />
            <div>
              <strong>{{ node.statusText }}</strong>
              <p v-if="node.remark">{{ node.remark }}</p>
              <small v-if="node.address">{{ node.address }}</small>
              <small>{{ formatDateTime(node.time) }}</small>
            </div>
          </li>
        </ol>
      </section>

      <section v-if="order.payment" class="detail-section surface-card">
        <div class="section-title">
          <h2>支付流水</h2>
          <van-tag :type="order.payment.status === 1 ? 'success' : 'warning'">
            {{ paymentStatusText }}
          </van-tag>
        </div>
        <dl class="payment-facts">
          <div><dt>流水号</dt><dd>{{ order.payment.payNo }}</dd></div>
          <div>
            <dt>支付方式</dt>
            <dd>{{ order.payment.payType === 1 ? '微信支付' : '支付宝' }}</dd>
          </div>
          <div v-if="order.payment.payTime">
            <dt>支付时间</dt>
            <dd>{{ formatDateTime(order.payment.payTime) }}</dd>
          </div>
        </dl>
      </section>

      <div class="detail-actions">
        <template v-if="order.status === 0">
          <van-button
            block
            plain
            type="danger"
            :loading="cancelling"
            @click="cancelCurrentOrder"
          >
            取消订单
          </van-button>
          <van-button
            block
            type="primary"
            :loading="paying"
            @click="goPay"
          >
            去支付
          </van-button>
        </template>
        <van-button v-else block plain @click="router.push('/orders')">
          返回订单列表
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useQuery, useQueryClient } from '@tanstack/vue-query';
import {
  cancelOrder,
  getOrderDetail,
  getOrderDeliveryTrace,
  initiatePayment,
} from '@/api/modules/trade';
import { isApiError } from '@/types/api/common';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'OrderDetailPage' });

const route = useRoute();
const router = useRouter();
const queryClient = useQueryClient();
const cancelling = ref(false);
const paying = ref(false);
const rawOrderNo = Array.isArray(route.params.orderNo)
  ? route.params.orderNo[0]
  : route.params.orderNo;
const orderNo = computed(() => rawOrderNo || '');
const validOrderNo = computed(() => /^FM[A-Za-z0-9]{8,32}$/.test(orderNo.value));

const {
  data: order,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ['shop', 'order', orderNo],
  queryFn: ({ signal }) => getOrderDetail(orderNo.value, signal),
  enabled: validOrderNo,
  refetchInterval: query =>
    query.state.data &&
    query.state.data.status >= 3 &&
    query.state.data.status <= 6
      ? 10_000
      : false,
});

const { data: deliveryTrace } = useQuery({
  queryKey: ['shop', 'order-trace', orderNo],
  queryFn: ({ signal }) => getOrderDeliveryTrace(orderNo.value, signal),
  enabled: computed(
    () =>
      validOrderNo.value &&
      !!order.value &&
      order.value.status >= 3 &&
      order.value.status <= 6,
  ),
  staleTime: 5000,
  refetchInterval: 10_000,
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '订单加载失败',
);

const statusDescription = computed(() => {
  if (!order.value) return '';
  const descriptions: Record<number, string> = {
    0: '请在支付有效期内完成模拟支付，超时后系统会自动取消并回补库存。',
    1: '支付成功，等待商家接单。',
    2: '商家正在备餐。',
    3: '等待预约配送时段开始。',
    4: '餐品已备好，等待配送。',
    5: '配送员正在配送。',
    6: '订单已送达并完成。',
    7: '退款处理中。',
    8: '退款已完成。',
    9: '订单已取消。',
  };
  return descriptions[order.value.status] ?? '';
});

const paymentStatusText = computed(() => {
  const status = order.value?.payment?.status;
  if (status === 1) return '支付成功';
  if (status === 2) return '支付失败';
  if (status === 3) return '已关闭';
  return '待支付';
});

async function cancelCurrentOrder() {
  if (!order.value || cancelling.value) return;
  try {
    await showConfirmDialog({
      title: '取消订单',
      message: '取消后会关闭待支付流水，并恢复商品与批次库存。',
      confirmButtonText: '确认取消',
      confirmButtonColor: 'var(--color-danger)',
    });
  } catch {
    return;
  }
  cancelling.value = true;
  try {
    await cancelOrder(orderNo.value);
    await queryClient.invalidateQueries({
      queryKey: ['shop', 'order', orderNo],
    });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'orders'] });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'cart'] });
    showToast({ message: '订单已取消', icon: 'success' });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '取消订单失败');
    await refetch();
  } finally {
    cancelling.value = false;
  }
}

async function goPay() {
  if (!order.value || paying.value) return;
  paying.value = true;
  try {
    const payment = await initiatePayment(orderNo.value, { payType: 1 });
    void router.push(`/pay/${orderNo.value}?payNo=${encodeURIComponent(payment.payNo)}`);
  } catch (error) {
    showToast(isApiError(error) ? error.message : '发起支付失败');
  } finally {
    paying.value = false;
  }
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped lang="scss">
.order-detail-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
  padding-bottom: calc(var(--space-8) + env(safe-area-inset-bottom));
}

.status-hero {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-lg);

  span,
  p {
    color: color-mix(in srgb, var(--color-primary-contrast) 75%, transparent);
  }

  h1,
  p {
    margin: var(--space-1) 0 0;
  }

  h1 {
    font-size: var(--text-page-title);
  }

  .van-icon {
    font-size: 2.5rem;
  }

  &--0 {
    background: var(--color-warning);
  }

  &--6,
  &--8 {
    background: var(--color-success);
  }

  &--9 {
    background: var(--color-danger);
  }
}

.detail-section {
  display: grid;
  gap: var(--space-3);
}

.section-title {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: var(--text-card-title);
  }

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
}

.detail-section > p {
  margin: 0;
}

.detail-section > small {
  color: var(--color-text-muted);
}

.item-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    display: grid;
    grid-template-columns: 3.75rem 1fr auto;
    gap: var(--space-3);
    align-items: center;
  }

  img {
    width: 3.75rem;
    height: 3.75rem;
    object-fit: cover;
    background: var(--color-background-soft);
    border-radius: var(--radius-md);
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

.amounts {
  display: grid;
  gap: var(--space-2);
  padding-top: var(--space-3);
  margin: 0;
  border-top: 1px solid var(--color-border);

  > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  dt {
    color: var(--color-text-secondary);
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

.timeline {
  display: grid;
  gap: 0;
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    position: relative;
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-3);
    min-height: 4.5rem;

    &:not(:last-child)::before {
      position: absolute;
      top: 1rem;
      bottom: -0.25rem;
      left: 0.375rem;
      width: 1px;
      content: '';
      background: var(--color-border-strong);
    }
  }

  &__dot {
    z-index: 1;
    width: 0.75rem;
    height: 0.75rem;
    margin-top: 0.375rem;
    background: var(--color-surface);
    border: 2px solid var(--color-border-strong);
    border-radius: var(--radius-full);
  }

  li.current .timeline__dot {
    background: var(--color-primary);
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px var(--color-primary-soft);
  }

  strong,
  p,
  small {
    display: block;
    margin: 0;
  }

  p {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  small {
    margin-top: var(--space-1);
    font-size: var(--text-caption);
    color: var(--color-text-muted);
  }
}

.payment-facts {
  display: grid;
  gap: var(--space-2);
  margin: 0;

  > div {
    display: flex;
    gap: var(--space-4);
    align-items: center;
    justify-content: space-between;
  }

  dt {
    color: var(--color-text-secondary);
  }

  dd {
    max-width: 65%;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: right;
    white-space: nowrap;
  }
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: var(--space-3);
}
</style>
