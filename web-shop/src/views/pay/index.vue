<template>
  <div class="pay-page page">
    <header class="page-header">
      <div>
        <span>模拟收银台</span>
        <h1>确认支付信息</h1>
        <p>本页面仅用于沙箱验证，不会产生真实扣款。</p>
      </div>
    </header>

    <div v-if="!validOrderNo" class="state-panel" role="alert">
      <p>支付链接无效，请从订单详情重新进入。</p>
      <RouterLink class="button-secondary" to="/orders">
        返回订单列表
      </RouterLink>
    </div>
    <div v-else-if="isLoading" class="state-panel">
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
          <li
            v-for="item in order.items"
            :key="`${item.productId}-${item.specName}`"
          >
            <span>{{ item.productName }} × {{ item.quantity }}</span>
            <b>{{ formatPrice(item.subtotal) }}</b>
          </li>
        </ul>
        <div class="fee-row">
          <span>配送费</span>
          <b>{{ formatPrice(order.freightAmount) }}</b>
        </div>
      </section>

      <section
        v-if="paymentSucceeded"
        class="pay-success surface-card"
        role="status"
      >
        <van-icon name="checked" />
        <div>
          <h2>支付成功</h2>
          <p v-if="order.payTime">
            支付时间 {{ formatDateTime(order.payTime) }}
          </p>
          <p v-else>订单已进入待接单状态。</p>
        </div>
        <van-button type="primary" @click="goDetail">查看订单</van-button>
      </section>

      <section v-else-if="order.status === 0" class="pay-section surface-card">
        <div v-if="paymentFailed" class="payment-alert" role="alert">
          <van-icon name="warning-o" />
          <div>
            <strong>本次模拟支付失败</strong>
            <p>支付流水已记录，可重新选择支付方式。</p>
          </div>
        </div>
        <div v-if="actionError" class="payment-alert" role="alert">
          <van-icon name="warning-o" />
          <div>
            <strong>操作未完成</strong>
            <p>{{ actionError }}</p>
          </div>
        </div>

        <h2>选择支付方式</h2>
        <div class="pay-methods" aria-label="模拟支付方式">
          <article
            v-for="method in payMethods"
            :key="method.payType"
            class="pay-method"
          >
            <div class="pay-method__heading">
              <span :class="method.markClass">{{ method.mark }}</span>
              <div>
                <strong>{{ method.name }}</strong>
                <small>沙箱环境，不会真实扣款</small>
              </div>
            </div>
            <div class="pay-method__actions">
              <button
                type="button"
                class="button-primary"
                :disabled="payingType !== null"
                :aria-busy="isPaying(method.payType, true)"
                @click="confirmPay(method.payType, true)"
              >
                {{
                  isPaying(method.payType, true) ? '处理中…' : '模拟支付成功'
                }}
              </button>
              <button
                type="button"
                class="button-secondary"
                :disabled="payingType !== null"
                :aria-busy="isPaying(method.payType, false)"
                @click="confirmPay(method.payType, false)"
              >
                {{
                  isPaying(method.payType, false) ? '处理中…' : '模拟支付失败'
                }}
              </button>
            </div>
          </article>
        </div>
        <p v-if="order.payment?.expireTime" class="sandbox-expiry">
          当前支付流水有效期至
          {{ formatDateTime(order.payment.expireTime) }}
        </p>
        <p class="sandbox-note">
          支付回调具备幂等处理：重复通知不会重复修改订单状态或库存。
        </p>
      </section>

      <section v-else class="pay-success surface-card" role="status">
        <van-icon name="orders-o" />
        <div>
          <h2>{{ order.statusText }}</h2>
          <p>订单当前无需在收银台支付。</p>
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
const payingOutcome = ref<boolean | null>(null);
const simulatedSuccess = ref(false);
const simulatedFailure = ref(false);
const actionError = ref('');
const rawOrderNo = Array.isArray(route.params.orderNo)
  ? route.params.orderNo[0]
  : route.params.orderNo;
const orderNo = computed(() => rawOrderNo || '');
const validOrderNo = computed(() =>
  /^FM[A-Za-z0-9]{8,32}$/.test(orderNo.value),
);
const payMethods = [
  {
    payType: 1 as const,
    name: '微信支付',
    mark: '微',
    markClass: 'wechat-mark',
  },
  { payType: 2 as const, name: '支付宝', mark: '支', markClass: 'alipay-mark' },
];

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
const paymentSucceeded = computed(
  () => simulatedSuccess.value || order.value?.payment?.status === 1,
);
const paymentFailed = computed(
  () => simulatedFailure.value || order.value?.payment?.status === 2,
);

watch(orderNo, () => {
  simulatedSuccess.value = false;
  simulatedFailure.value = false;
  actionError.value = '';
});

function isPaying(payType: 1 | 2, success: boolean) {
  return payingType.value === payType && payingOutcome.value === success;
}

async function confirmPay(payType: 1 | 2, success: boolean) {
  if (!order.value || payingType.value !== null || order.value.status !== 0) {
    return;
  }
  payingType.value = payType;
  payingOutcome.value = success;
  actionError.value = '';
  try {
    await showConfirmDialog({
      title: success ? '确认模拟支付成功' : '确认模拟支付失败',
      message: success
        ? `将使用${payType === 1 ? '微信支付' : '支付宝'}支付 ${formatPrice(order.value.payAmount)}，并模拟成功回调。`
        : `将使用${payType === 1 ? '微信支付' : '支付宝'}发起支付，并模拟失败回调。`,
      confirmButtonText: success ? '确认支付' : '确认失败',
      cancelButtonText: '取消',
    });
    const payment = await initiatePayment(orderNo.value, { payType });
    await notifyMockPayment({
      payNo: payment.payNo,
      payType,
      success,
      ...(success ? { thirdPartyNo: `MOCK-${Date.now()}` } : {}),
    });
    simulatedSuccess.value = success;
    simulatedFailure.value = !success;
    await queryClient.invalidateQueries({
      queryKey: ['shop', 'order', orderNo],
    });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'orders'] });
    showToast({
      message: success ? '支付成功' : '已记录模拟支付失败',
      icon: success ? 'success' : 'warning',
    });
  } catch (caughtError) {
    if (isApiError(caughtError)) {
      actionError.value = caughtError.message;
      showToast({ message: caughtError.message, icon: 'warning' });
    }
  } finally {
    payingType.value = null;
    payingOutcome.value = null;
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
}

.pay-method {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.pay-method__heading {
  display: flex;
  gap: var(--space-3);
  align-items: center;

  div {
    display: grid;
    gap: var(--space-1);
    min-width: 0;
  }

  small {
    color: var(--color-text-secondary);
  }
}

.pay-method__actions {
  display: grid;
  gap: var(--space-2);

  button {
    width: 100%;
    min-height: var(--touch-target);

    &:disabled {
      cursor: wait;
      opacity: 0.65;
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

.sandbox-expiry {
  margin: 0;
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.payment-alert {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  padding: var(--space-3);
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 8%, transparent);
  border: 1px solid
    color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
  border-radius: var(--radius-md);

  .van-icon {
    flex: 0 0 auto;
    font-size: 1.25rem;
  }

  strong,
  p {
    display: block;
    margin: 0;
  }

  p {
    margin-top: var(--space-1);
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
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
