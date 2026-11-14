<template>
  <div class="orders-page page">
    <header class="page-header">
      <div>
        <span>我的订单</span>
        <h1>查看交易与订单状态</h1>
        <p>订单状态与支付流水均由服务端状态机统一维护。</p>
      </div>
    </header>

    <div class="status-tabs no-scrollbar" role="tablist" aria-label="订单状态筛选">
      <button
        v-for="tab in statusTabs"
        :key="tab.label"
        type="button"
        role="tab"
        :aria-selected="activeStatus === tab.status"
        :class="{ active: activeStatus === tab.status }"
        @click="selectStatus(tab.status)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>订单加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <van-empty
      v-else-if="orders && orders.records.length === 0"
      description="还没有符合条件的订单"
    >
      <RouterLink class="empty-action" to="/catalog">去选餐</RouterLink>
    </van-empty>

    <ul v-else-if="orders" class="order-list">
      <li
        v-for="order in orders.records"
        :key="order.id"
        class="order-card surface-card"
        role="link"
        tabindex="0"
        @click="openDetail(order.orderNo)"
        @keydown.enter="openDetail(order.orderNo)"
      >
        <div class="order-card__head">
          <span>{{ formatDateTime(order.createTime) }}</span>
          <b :class="`status-${order.status}`">{{ order.statusText }}</b>
        </div>
        <div class="order-card__body">
          <img
            v-if="order.items[0]?.productCover"
            :src="order.items[0].productCover"
            :alt="order.items[0].productName"
          />
          <div>
            <strong>{{ order.items[0]?.productName || '订单商品' }}</strong>
            <p v-if="order.items.length > 1">
              等 {{ order.items.length }} 种商品 ·
              共 {{ order.items.reduce((sum, item) => sum + item.quantity, 0) }} 件
            </p>
            <p>{{ order.receiverAddress }}</p>
          </div>
          <b>{{ formatPrice(order.payAmount) }}</b>
        </div>
        <div class="order-card__actions" @click.stop>
          <span>订单号 {{ order.orderNo }}</span>
          <van-button
            v-if="order.status === 0"
            size="small"
            type="primary"
            plain
            @click="goPay(order.orderNo)"
          >
            立即支付
          </van-button>
          <van-button
            v-else
            size="small"
            plain
            @click="openDetail(order.orderNo)"
          >
            查看详情
          </van-button>
        </div>
      </li>
    </ul>

    <van-pagination
      v-if="orders && orders.pages > 1"
      v-model="current"
      :total-items="orders.total"
      :items-per-page="size"
      mode="simple"
      @change="refetch"
    />
  </div>
</template>

<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import { getOrders, initiatePayment } from '@/api/modules/trade';
import { isApiError } from '@/types/api/common';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'OrdersPage' });

const statusTabs = [
  { label: '全部', status: undefined },
  { label: '待支付', status: 0 },
  { label: '待接单', status: 1 },
  { label: '待配送', status: 4 },
  { label: '配送中', status: 5 },
  { label: '已完成', status: 6 },
] as const;

const activeStatus = ref<number | undefined>(undefined);
const current = ref(1);
const size = 10;
const router = useRouter();

const {
  data: orders,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ['shop', 'orders', activeStatus, current, size],
  queryFn: ({ signal }) =>
    getOrders({ status: activeStatus.value, current: current.value, size }, signal),
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '订单加载失败',
);

function selectStatus(status: number | undefined) {
  activeStatus.value = status;
  current.value = 1;
}

function openDetail(orderNo: string) {
  void router.push(`/orders/${orderNo}`);
}

async function goPay(orderNo: string) {
  try {
    const payment = await initiatePayment(orderNo, { payType: 1 });
    void router.push(`/pay/${orderNo}?payNo=${encodeURIComponent(payment.payNo)}`);
  } catch (error) {
    showToast(isApiError(error) ? error.message : '发起支付失败');
  }
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      });
}
</script>

<style scoped lang="scss">
.orders-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
}

.status-tabs {
  display: flex;
  gap: var(--space-2);
  padding-bottom: var(--space-1);
  overflow-x: auto;

  button {
    flex: 0 0 auto;
    min-height: var(--touch-target);
    padding: 0 var(--space-3);
    color: var(--color-text-secondary);
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-full);

    &.active {
      font-weight: 700;
      color: var(--color-primary-contrast);
      background: var(--color-primary);
      border-color: var(--color-primary);
    }
  }
}

.order-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;
}

.order-card {
  display: grid;
  gap: var(--space-3);
  cursor: pointer;
}

.order-card__head,
.order-card__actions {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
}

.order-card__head {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);

  b {
    color: var(--color-primary);
  }
}

.order-card__body {
  display: grid;
  grid-template-columns: 4rem 1fr auto;
  gap: var(--space-3);
  align-items: center;

  img {
    width: 4rem;
    height: 4rem;
    object-fit: cover;
    background: var(--color-background-soft);
    border-radius: var(--radius-md);
  }

  strong {
    display: block;
  }

  p {
    margin: var(--space-1) 0 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  > b {
    font-size: var(--text-section-title);
    color: var(--color-primary);
  }
}

.order-card__actions {
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: var(--text-caption);
    color: var(--color-text-muted);
    white-space: nowrap;
  }
}

.status-0 {
  color: var(--color-warning) !important;
}

.status-6,
.status-8 {
  color: var(--color-success) !important;
}

.status-9 {
  color: var(--color-danger) !important;
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
</style>
