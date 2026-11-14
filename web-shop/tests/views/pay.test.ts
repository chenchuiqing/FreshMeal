import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import PayPage from '@/views/pay/index.vue';
import { ApiError } from '@/types/api/common';

const mocks = vi.hoisted(() => ({
  getOrderDetail: vi.fn(),
  initiatePayment: vi.fn(),
  notifyMockPayment: vi.fn(),
  showConfirmDialog: vi.fn(),
  showToast: vi.fn(),
  invalidateQueries: vi.fn(),
  queryState: {
    order: null as unknown,
    isLoading: null as unknown,
    isError: null as unknown,
    error: null as unknown,
    refetch: vi.fn(),
  },
}));

vi.mock('@/api/modules/trade', () => ({
  getOrderDetail: mocks.getOrderDetail,
  initiatePayment: mocks.initiatePayment,
  notifyMockPayment: mocks.notifyMockPayment,
}));

vi.mock('@tanstack/vue-query', () => ({
  useQuery: () => ({
    data: mocks.queryState.order,
    isLoading: mocks.queryState.isLoading,
    isError: mocks.queryState.isError,
    error: mocks.queryState.error,
    refetch: mocks.queryState.refetch,
  }),
  useQueryClient: () => ({
    invalidateQueries: mocks.invalidateQueries,
  }),
}));

vi.mock('vant/es', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vant/es')>();
  return {
    ...actual,
    showConfirmDialog: mocks.showConfirmDialog,
    showToast: mocks.showToast,
  };
});

function createOrder(status = 0, paymentStatus = 0) {
  return {
    id: 1,
    orderNo: 'FM12345678',
    status,
    statusText: status === 0 ? '待支付' : '待接单',
    payStatus: paymentStatus === 1 ? 1 : 0,
    payType: 0,
    deliveryType: 1,
    goodsAmount: 31,
    freightAmount: 5,
    discountAmount: 0,
    payAmount: 36,
    receiver: '测试用户',
    receiverPhone: '13800000001',
    receiverAddress: '上海市浦东新区测试路1号',
    createTime: '2026-11-14T10:00:00',
    payTime: paymentStatus === 1 ? '2026-11-14T10:01:00' : undefined,
    payment:
      paymentStatus === 0
        ? {
            id: 10,
            payNo: 'PAY1234567890',
            orderNo: 'FM12345678',
            payType: 1,
            status: paymentStatus,
            amount: 36,
            expireTime: '2026-11-14T10:15:00',
          }
        : {
            id: 10,
            payNo: 'PAY1234567890',
            orderNo: 'FM12345678',
            payType: 1,
            status: paymentStatus,
            amount: 36,
            payTime: '2026-11-14T10:01:00',
          },
    items: [
      {
        productId: 1,
        productName: '鸡胸肉沙拉',
        specName: '标准份',
        quantity: 1,
        subtotal: 31,
      },
    ],
    timeline: [],
  };
}

async function mountPayPage(orderNo = 'FM12345678') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pay/:orderNo', component: PayPage },
      { path: '/orders', component: { template: '<div />' } },
      { path: '/orders/:orderNo', component: { template: '<div />' } },
    ],
  });
  await router.push(`/pay/${orderNo}`);
  await router.isReady();
  return mount(PayPage, { global: { plugins: [router] } });
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.queryState.order = ref(createOrder());
  mocks.queryState.isLoading = ref(false);
  mocks.queryState.isError = ref(false);
  mocks.queryState.error = ref(null);
  mocks.showConfirmDialog.mockResolvedValue('confirm');
  mocks.invalidateQueries.mockResolvedValue(undefined);
});

describe('shop payment sandbox page', () => {
  it('shows an explicit state for an invalid payment link', async () => {
    const wrapper = await mountPayPage('invalid');

    expect(wrapper.text()).toContain('支付链接无效');
    expect(mocks.getOrderDetail).not.toHaveBeenCalled();
  });

  it('simulates a successful payment and shows completion feedback', async () => {
    mocks.initiatePayment.mockResolvedValue({ payNo: 'PAY1234567890' });
    mocks.notifyMockPayment.mockResolvedValue({ status: 1 });
    const wrapper = await mountPayPage();

    const successButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '模拟支付成功');
    if (!successButton) throw new Error('success action not found');
    await successButton.trigger('click');
    await flushPromises();

    expect(mocks.showConfirmDialog).toHaveBeenCalledOnce();
    expect(mocks.initiatePayment).toHaveBeenCalledWith('FM12345678', {
      payType: 1,
    });
    expect(mocks.notifyMockPayment).toHaveBeenCalledWith(
      expect.objectContaining({ payNo: 'PAY1234567890', success: true }),
    );
    expect(mocks.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ message: '支付成功' }),
    );
    expect(wrapper.text()).toContain('支付成功');
    expect(wrapper.text()).toContain('订单已进入待接单状态');
  });

  it('simulates a failed payment and keeps the cashier actionable', async () => {
    mocks.initiatePayment.mockResolvedValue({ payNo: 'PAY1234567890' });
    mocks.notifyMockPayment.mockResolvedValue({ status: 2 });
    const wrapper = await mountPayPage();

    const failureButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '模拟支付失败');
    if (!failureButton) throw new Error('failure action not found');
    await failureButton.trigger('click');
    await flushPromises();

    expect(mocks.notifyMockPayment).toHaveBeenCalledWith(
      expect.objectContaining({ success: false }),
    );
    expect(wrapper.text()).toContain('本次模拟支付失败');
    expect(wrapper.text()).toContain('模拟支付成功');
    expect(mocks.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ message: '已记录模拟支付失败' }),
    );
  });

  it('does not initiate payment when confirmation is cancelled', async () => {
    mocks.showConfirmDialog.mockRejectedValue(new Error('cancel'));
    const wrapper = await mountPayPage();

    const successButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '模拟支付成功');
    if (!successButton) throw new Error('success action not found');
    await successButton.trigger('click');
    await flushPromises();

    expect(mocks.initiatePayment).not.toHaveBeenCalled();
    expect(mocks.notifyMockPayment).not.toHaveBeenCalled();
    expect(wrapper.text()).not.toContain('操作未完成');
  });

  it('surfaces payment API failures without leaving the cashier stuck', async () => {
    mocks.initiatePayment.mockRejectedValue(
      new ApiError('支付服务暂不可用', { kind: 'network' }),
    );
    const wrapper = await mountPayPage();

    const successButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '模拟支付成功');
    if (!successButton) throw new Error('success action not found');
    await successButton.trigger('click');
    await flushPromises();

    expect(wrapper.text()).toContain('操作未完成');
    expect(wrapper.text()).toContain('支付服务暂不可用');
    expect(mocks.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ message: '支付服务暂不可用' }),
    );
    expect(successButton.attributes('disabled')).toBeUndefined();
  });
});
