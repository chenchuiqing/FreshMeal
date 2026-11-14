<template>
  <div class="pay-page page">
    <header class="page-header">
      <div>
        <span>模拟收银台</span>
        <h1>确认支付信息</h1>
        <p>本页面仅用于沙箱验证，不会产生真实扣款。</p>
      </div>
    </header>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>订单加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <template v-else-if="order">
      <section class="amount-card">
        <span>{{ order.statusText }}</span>
        <strong>{{ formatPrice(order.payAmount) }}</strong>
        <small>订单号 {{ order.orderNo }}</small>
      </section>

      <section class="pay-section surface-card">
        <h2>支付商品</h2>
        <ul>
          <li v-for="item in order.items" :key="`${item.productId}-${item.specName}`">
            <span>{{ item.productName }} × {{ item.quantity }}</span>
            <b>{{ formatPrice(item.subtotal) }}</b>
          </li>
        </ul>
        <div class="fee-row">
          <span>配送费</span>
          <b>{{ formatPrice(order.freightAmount) }}</b>
        </div>
      </section>

      <section v-if="order.status === 0" class="pay-section surface-card">
        <h2>选择支付方式</h2>
        <div class="pay-methods">
          <button type="button" @click="confirmPay(1)">
            <span class="wechat-mark">微</span>
            <strong>微信支付</strong>
            <small>模拟成功回调</small>
          </button>
          <button type="button" @click="confirmPay(2)">
            <span class="alipay-mark">支</span>
            <strong>支付宝</strong>
            <small>模拟成功回调</small>
          </button>
        </div>
        <p class="sandbox-note">
          支付回调具备幂等处理：重复通知不会重复修改订单状态或库存。
        </p>
      </section>

      <section v-else class="pay-success surface-card" role="status">
        <van-icon name="checked" />
        <div>
          <h2>支付成功</h2>
          <p v-if="order.payTime">
            支付时间 {{ formatDateTime(order.payTime) }}
          </p>
        </div>
        <van-button type="primary" @click="goDetail">查看订单</van-button>
      </section>

      <RouterLink
        v-if="order.status === 0"
        class="cancel-link"
        :to="`/orders/${order.orderNo}`"
      >
        返回订单详情
      </RouterLink>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useQuery, useQueryClient } from '@tanstack/vue-query';
import {
  getOrderDetail,
  initiatePayment,
  notifyMockPayment,
} from '@/api/modules/trade';
import { isApiError } from '@/types/api/common';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'PayPage' });

const route = useRoute();
const router = useRouter();
const queryClient = useQueryClient();
const payingType = ref<number | null>(null);
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
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '订单加载失败',
);

async function confirmPay(payType: 1 | 2) {
  if (!order.value || payingType.value !== null || order.value.status !== 0) {
    return;
  }
  payingType.value = payType;
  try {
    await showConfirmDialog({
      title: '确认模拟支付',
      message: `将使用${payType === 1 ? '微信支付' : '支付宝'}支付 ${formatPrice(order.value.payAmount)}。`,
      confirmButtonText: '确认支付',
    });
    const payment = await initiatePayment(orderNo.value, { payType });
    await notifyMockPayment({
      payNo: payment.payNo,
      payType,
      success: true,
      thirdPartyNo: `MOCK-${Date.now()}`,
    });
    await queryClient.invalidateQueries({
      queryKey: ['shop', 'order', orderNo],
    });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'orders'] });
    showToast({ message: '支付成功', icon: 'success' });
    window.setTimeout(() => goDetail(), 500);
  } catch (error) {
    if (isApiError(error)) {
      showToast(error.message);
    }
  } finally {
    payingType.value = null;
  }
}

function goDetail() {
  void router.replace(`/orders/${orderNo.value}`);
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped lang="scss">
.pay-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
}

.amount-card {
  display: grid;
  gap: var(--space-2);
  place-items: center;
  padding: var(--space-7) var(--space-4);
  color: var(--color-primary-contrast);
  text-align: center;
  background: var(--color-primary);
  border-radius: var(--radius-lg);

  span,
  small {
    color: color-mix(in srgb, var(--color-primary-contrast) 75%, transparent);
  }

  strong {
    font-size: 2.5rem;
    line-height: 1;
    letter-spacing: -0.04em;
  }
}

.pay-section {
  display: grid;
  gap: var(--space-3);

  h2 {
    margin: 0;
    font-size: var(--text-card-title);
  }

  ul {
    display: grid;
    gap: var(--space-2);
    padding: 0;
    margin: 0;
    list-style: none;
  }

  li,
  .fee-row {
    display: flex;
    gap: var(--space-4);
    align-items: center;
    justify-content: space-between;
  }

  li span,
  .fee-row span {
    color: var(--color-text-secondary);
  }
}

.fee-row {
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.pay-methods {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);

  button {
    display: grid;
    gap: var(--space-2);
    place-items: center;
    min-height: 8.5rem;
    padding: var(--space-4);
    color: var(--color-text);
    background: var(--color-background-soft);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);

    &:hover {
      border-color: var(--color-primary);
    }

    small {
      color: var(--color-text-secondary);
    }
  }
}

.wechat-mark,
.alipay-mark {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  font-size: 1.25rem;
  font-weight: 800;
  color: #fff;
  border-radius: var(--radius-full);
}

.wechat-mark {
  background: #1aad19;
}

.alipay-mark {
  background: #1677ff;
}

.sandbox-note {
  margin: 0;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.pay-success {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-3);
  align-items: center;

  .van-icon {
    font-size: 2rem;
    color: var(--color-success);
  }

  h2,
  p {
    margin: 0;
  }

  p {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  .van-button {
    grid-column: 1 / -1;
  }
}

.cancel-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--touch-target);
  color: var(--color-text-secondary);
  text-decoration: none;
}
</style>
