<template>
  <div class="coupon-page page">
    <header class="page-header">
      <div>
        <span>营销中心</span>
        <h1>我的优惠券</h1>
        <p>领取优惠券后，结算时选择可用券即可抵扣。</p>
      </div>
      <RouterLink class="text-button" to="/member">返回我的</RouterLink>
    </header>

    <section class="surface-card content-section">
      <div class="section-title">
        <h2>可领取</h2>
        <van-button
          plain
          size="small"
          :loading="availableLoading"
          @click="refetchAvailable"
        >
          刷新
        </van-button>
      </div>
      <div v-if="availableLoading" class="state-panel">
        <van-loading vertical>优惠券加载中…</van-loading>
      </div>
      <div v-else-if="availableError" class="state-panel" role="alert">
        <p>{{ availableErrorText }}</p>
        <van-button plain type="primary" @click="refetchAvailable">
          重新加载
        </van-button>
      </div>
      <van-empty
        v-else-if="!available?.length"
        description="暂时没有可领取的优惠券"
      />
      <div v-else class="coupon-list">
        <article
          v-for="coupon in available"
          :key="coupon.id"
          class="coupon-card"
        >
          <div class="coupon-card__value">
            <strong>{{ couponValue(coupon) }}</strong>
            <span>{{ coupon.type === 1 ? '满减券' : '折扣券' }}</span>
          </div>
          <div class="coupon-card__info">
            <h3>{{ coupon.name }}</h3>
            <p>满 {{ formatPrice(coupon.threshold) }} 可用</p>
            <small>有效期至 {{ formatDateTime(coupon.endTime) }}</small>
          </div>
          <van-button
            type="primary"
            size="small"
            :loading="receivingId === coupon.id"
            :disabled="coupon.receivedCount >= coupon.totalCount"
            @click="receive(coupon)"
          >
            {{ coupon.receivedCount >= coupon.totalCount ? '已领完' : '领取' }}
          </van-button>
        </article>
      </div>
    </section>

    <section class="surface-card content-section">
      <div class="section-title">
        <h2>我的券</h2>
        <van-button
          plain
          size="small"
          :loading="mineLoading"
          @click="refetchMine"
        >
          刷新
        </van-button>
      </div>
      <div v-if="mineLoading" class="state-panel">
        <van-loading vertical>我的优惠券加载中…</van-loading>
      </div>
      <div v-else-if="mineError" class="state-panel" role="alert">
        <p>{{ mineErrorText }}</p>
        <van-button plain type="primary" @click="refetchMine">
          重新加载
        </van-button>
      </div>
      <van-empty
        v-else-if="!mine?.length"
        description="还没有优惠券，先领取一张吧"
      />
      <div v-else class="mine-list">
        <article
          v-for="coupon in mine"
          :key="coupon.id"
          class="mine-card"
          :class="`mine-card--${couponStatus(coupon.status)}`"
        >
          <div>
            <h3>{{ coupon.name }}</h3>
            <p>
              满 {{ formatPrice(coupon.threshold) }} 减
              {{ couponValue(coupon) }}
            </p>
            <small>有效期至 {{ formatDateTime(coupon.expireTime) }}</small>
          </div>
          <van-tag :type="couponTagType(coupon.status)">
            {{ couponStatusText(coupon.status) }}
          </van-tag>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useQuery, useQueryClient } from '@tanstack/vue-query';
import {
  listAvailableCoupons,
  listUserCoupons,
  receiveCoupon,
} from '@/api/modules/marketing';
import type { Coupon, UserCoupon } from '@/api/modules/marketing';
import { isApiError } from '@/types/api/common';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'CouponsPage' });

const queryClient = useQueryClient();
const receivingId = ref<number | null>(null);

const {
  data: available,
  isLoading: availableLoading,
  isError: availableError,
  error: availableQueryError,
  refetch: refetchAvailable,
} = useQuery({
  queryKey: ['shop', 'coupons', 'available'],
  queryFn: ({ signal }) => listAvailableCoupons(signal),
});

const {
  data: mine,
  isLoading: mineLoading,
  isError: mineError,
  error: mineQueryError,
  refetch: refetchMine,
} = useQuery({
  queryKey: ['shop', 'coupons', 'mine'],
  queryFn: ({ signal }) => listUserCoupons(signal),
});

const availableErrorText = computed(() =>
  isApiError(availableQueryError.value)
    ? availableQueryError.value.message
    : '优惠券加载失败',
);
const mineErrorText = computed(() =>
  isApiError(mineQueryError.value)
    ? mineQueryError.value.message
    : '我的优惠券加载失败',
);

async function receive(coupon: Coupon) {
  if (receivingId.value !== null) return;
  receivingId.value = coupon.id;
  try {
    await receiveCoupon(coupon.id);
    showToast({ message: '领取成功', icon: 'success' });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'coupons'] });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '领取失败，请稍后重试');
  } finally {
    receivingId.value = null;
  }
}

function couponValue(coupon: Coupon | UserCoupon) {
  return coupon.type === 1
    ? formatPrice(coupon.amount)
    : `${Math.round(Number(coupon.amount) * 10)}折`;
}

function couponStatus(status: number) {
  if (status === 0) return 'unused';
  return status === 1 ? 'used' : 'expired';
}

function couponStatusText(status: number) {
  if (status === 0) return '未使用';
  return status === 1 ? '已使用' : '已过期';
}

function couponTagType(status: number) {
  if (status === 0) return 'success';
  return status === 1 ? 'default' : 'danger';
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped lang="scss">
.coupon-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
}

.content-section {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-4);
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
}

.coupon-list,
.mine-list {
  display: grid;
  gap: var(--space-3);
}

.coupon-card {
  display: grid;
  grid-template-columns: 5.5rem minmax(0, 1fr) auto;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-3);
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.coupon-card__value {
  display: grid;
  gap: var(--space-1);
  place-items: center;
  min-height: 4.5rem;
  color: var(--color-danger);
  text-align: center;
  background: color-mix(in srgb, var(--color-danger) 8%, transparent);
  border-radius: var(--radius-sm);

  strong {
    font-size: var(--text-section-title);
  }

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }
}

.coupon-card__info,
.mine-card > div {
  display: grid;
  gap: var(--space-1);
  min-width: 0;

  h3,
  p {
    margin: 0;
  }

  p,
  small {
    color: var(--color-text-secondary);
  }
}

.mine-card {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  &--used,
  &--expired {
    opacity: 0.65;
  }
}

@media (max-width: 420px) {
  .coupon-card {
    grid-template-columns: 4.75rem minmax(0, 1fr);

    .van-button {
      grid-column: 1 / -1;
    }
  }
}
</style>
