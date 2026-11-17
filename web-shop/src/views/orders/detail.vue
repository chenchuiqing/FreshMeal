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

      <section
        v-if="order.status === 6 || reviews?.length"
        class="detail-section surface-card"
      >
        <div class="section-title">
          <h2>订单评价</h2>
          <van-button
            v-if="canReview"
            plain
            size="small"
            type="primary"
            @click="reviewFormVisible = !reviewFormVisible"
          >
            {{ reviewFormVisible ? '收起' : '写评价' }}
          </van-button>
        </div>
        <div v-if="reviews?.length" class="review-list">
          <article v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-item__head">
              <strong>{{ review.productName }}</strong>
              <van-rate :model-value="review.rating" readonly size="14px" />
            </div>
            <p v-if="review.content">{{ review.content }}</p>
            <small>{{ formatDateTime(review.createTime) }}</small>
            <p v-if="review.reply" class="review-reply">
              商家回复：{{ review.reply }}
            </p>
          </article>
        </div>
        <p v-else class="empty-hint">订单完成后即可评价商品。</p>

        <form v-if="reviewFormVisible" class="inline-form" @submit.prevent="submitReview">
          <label>
            评价商品
            <van-radio-group v-model="reviewProductId" class="review-products">
              <van-radio
                v-for="item in reviewableItems"
                :key="item.productId"
                :name="item.productId"
              >
                {{ item.productName }}
              </van-radio>
            </van-radio-group>
          </label>
          <label>
            评分
            <van-rate v-model="reviewRating" />
          </label>
          <label>
            评价内容
            <van-field
              v-model="reviewContent"
              type="textarea"
              rows="3"
              maxlength="500"
              show-word-limit
              placeholder="分享一下这次用餐体验（选填）"
            />
          </label>
          <van-button
            block
            type="primary"
            native-type="submit"
            :loading="submittingReview"
          >
            提交评价
          </van-button>
        </form>
      </section>

      <section
        v-if="currentRefund || refundAllowed"
        class="detail-section surface-card"
      >
        <div class="section-title">
          <h2>售后退款</h2>
          <van-tag v-if="currentRefund" :type="refundTagType">
            {{ refundStatusText }}
          </van-tag>
        </div>
        <template v-if="currentRefund">
          <p>{{ currentRefund.reason }}</p>
          <p v-if="currentRefund.adminReply" class="refund-reply">
            处理说明：{{ currentRefund.adminReply }}
          </p>
          <small>申请时间 {{ formatDateTime(currentRefund.createTime) }}</small>
        </template>
        <template v-else>
          <p>订单已支付，如遇商品或配送问题，可提交售后申请。</p>
          <form v-if="refundFormVisible" class="inline-form" @submit.prevent="submitRefund">
            <van-field
              v-model="refundReason"
              type="textarea"
              rows="3"
              maxlength="500"
              show-word-limit
              placeholder="请描述需要退款的原因"
            />
            <van-button
              block
              type="danger"
              native-type="submit"
              :loading="submittingRefund"
            >
              提交退款申请
            </van-button>
          </form>
          <van-button
            v-else
            plain
            type="danger"
            @click="refundFormVisible = true"
          >
            申请售后退款
          </van-button>
        </template>
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
        <template v-else-if="order.status === 1">
          <van-button
            block
            plain
            type="danger"
            :loading="cancelling"
            @click="cancelCurrentOrder"
          >
            取消订单
          </van-button>
          <van-button block plain @click="router.push('/orders')">
            返回订单列表
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
import {
  listOrderReviews,
  listRefunds,
  requestRefund,
  submitOrderReview,
} from '@/api/modules/marketing';
import { isApiError } from '@/types/api/common';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'OrderDetailPage' });

const route = useRoute();
const router = useRouter();
const queryClient = useQueryClient();
const cancelling = ref(false);
const paying = ref(false);
const reviewFormVisible = ref(false);
const reviewProductId = ref<number | undefined>(undefined);
const reviewRating = ref(5);
const reviewContent = ref('');
const submittingReview = ref(false);
const refundFormVisible = ref(false);
const refundReason = ref('');
const submittingRefund = ref(false);
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

const { data: reviews } = useQuery({
  queryKey: ['shop', 'order-reviews', orderNo],
  queryFn: ({ signal }) => listOrderReviews(orderNo.value, signal),
  enabled: computed(() => validOrderNo.value && !!order.value),
});

const { data: refunds } = useQuery({
  queryKey: ['shop', 'refunds'],
  queryFn: ({ signal }) => listRefunds(signal),
  enabled: computed(() => validOrderNo.value && !!order.value),
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

const reviewedProductIds = computed(
  () => new Set((reviews.value ?? []).map(review => review.productId)),
);
const reviewableItems = computed(
  () =>
    (order.value?.items ?? []).filter(
      item => !reviewedProductIds.value.has(item.productId),
    ),
);
const canReview = computed(
  () => order.value?.status === 6 && reviewableItems.value.length > 0,
);
const currentRefund = computed(() =>
  refunds.value?.find(refund => refund.orderNo === orderNo.value),
);
const refundAllowed = computed(() => {
  const currentOrder = order.value;
  return Boolean(
    currentOrder &&
      currentOrder.payStatus === 1 &&
      currentOrder.status >= 1 &&
      currentOrder.status <= 6 &&
      !currentRefund.value,
  );
});
const refundStatusText = computed(() => {
  const status = currentRefund.value?.status;
  if (status === 0) return '处理中';
  return status === 1 ? '已退款' : '已驳回';
});
const refundTagType = computed(() => {
  const status = currentRefund.value?.status;
  if (status === 0) return 'warning';
  return status === 1 ? 'success' : 'danger';
});

watch(
  reviewableItems,
  items => {
    if (!reviewProductId.value || !items.some(item => item.productId === reviewProductId.value)) {
      reviewProductId.value = items[0]?.productId;
    }
  },
  { immediate: true },
);

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

async function submitReview() {
  const product = reviewableItems.value.find(
    item => item.productId === reviewProductId.value,
  );
  if (!product || submittingReview.value) return;
  submittingReview.value = true;
  try {
    await submitOrderReview(orderNo.value, {
      productId: product.productId,
      rating: reviewRating.value,
      content: reviewContent.value.trim() || undefined,
      images: [],
    });
    showToast({ message: '评价已提交', icon: 'success' });
    reviewFormVisible.value = false;
    reviewContent.value = '';
    await queryClient.invalidateQueries({
      queryKey: ['shop', 'order-reviews', orderNo],
    });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '评价提交失败');
  } finally {
    submittingReview.value = false;
  }
}

async function submitRefund() {
  if (!refundReason.value.trim() || submittingRefund.value) return;
  submittingRefund.value = true;
  try {
    await requestRefund(orderNo.value, { reason: refundReason.value.trim() });
    showToast({ message: '退款申请已提交', icon: 'success' });
    refundFormVisible.value = false;
    refundReason.value = '';
    await queryClient.invalidateQueries({
      queryKey: ['shop', 'order', orderNo],
    });
    await queryClient.invalidateQueries({ queryKey: ['shop', 'refunds'] });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '退款申请失败');
  } finally {
    submittingRefund.value = false;
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

.review-list {
  display: grid;
  gap: var(--space-3);
}

.review-item {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  p,
  small {
    margin: 0;
  }

  small {
    color: var(--color-text-muted);
  }
}

.review-item__head {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
}

.review-reply,
.refund-reply {
  padding: var(--space-2);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border-radius: var(--radius-sm);
}

.empty-hint {
  color: var(--color-text-secondary);
}

.inline-form {
  display: grid;
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);

  label {
    display: grid;
    gap: var(--space-2);
    color: var(--color-text-secondary);
  }
}

.review-products {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: var(--space-3);
}
</style>
