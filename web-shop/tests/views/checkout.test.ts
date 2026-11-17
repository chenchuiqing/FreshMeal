import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { ref } from 'vue';
import CheckoutPage from '@/views/checkout/index.vue';
import { useCheckoutStore } from '@/store/modules/checkout';
import { useUserStore } from '@/store/modules/user';

const mocks = vi.hoisted(() => ({
  settleCart: vi.fn(),
  createOrder: vi.fn(),
  getCart: vi.fn(),
  listAddresses: vi.fn(),
  listUserCoupons: vi.fn(),
  getAvailableDeliverySlots: vi.fn(),
  showToast: vi.fn(),
  invalidateQueries: vi.fn(),
  queryState: {
    settle: null as unknown,
    settleLoading: false,
    settleFailed: false,
    settleError: null as unknown,
    refetchSettle: vi.fn(),
    addresses: [] as unknown,
    slots: [] as unknown,
    coupons: [] as unknown,
    cart: null as unknown,
  },
}));

vi.mock('@/api/modules/trade', () => ({
  cartQueryKey: ['shop', 'cart'],
  settleCart: mocks.settleCart,
  createOrder: mocks.createOrder,
  getCart: mocks.getCart,
  listAddresses: mocks.listAddresses,
  getAvailableDeliverySlots: mocks.getAvailableDeliverySlots,
}));

vi.mock('@/api/modules/marketing', () => ({
  listUserCoupons: mocks.listUserCoupons,
}));

vi.mock('@tanstack/vue-query', () => ({
  useQuery: (options: {
    queryKey: unknown[];
    queryFn?: (context: { signal: AbortSignal }) => unknown;
    enabled?: unknown;
  }) => {
    const key = options.queryKey.join('|');
    // 真实调用查询函数，才能断言接口入参；enabled 为假时跳过，与真实行为一致。
    const enabled = resolveEnabled(options.enabled);
    if (options.queryFn && enabled) {
      void options.queryFn({ signal: new AbortController().signal });
    }
    if (key.includes('settle')) {
      return {
        data: mocks.queryState.settle,
        isLoading: mocks.queryState.settleLoading,
        isError: mocks.queryState.settleFailed,
        error: mocks.queryState.settleError,
        refetch: mocks.queryState.refetchSettle,
      };
    }
    if (key.includes('address')) {
      return {
        data: mocks.queryState.addresses,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
      };
    }
    if (key.includes('delivery-slots')) {
      return {
        data: mocks.queryState.slots,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
      };
    }
    if (key.includes('coupons')) {
      return {
        data: mocks.queryState.coupons,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
      };
    }
    if (key.includes('cart')) {
      return {
        data: mocks.queryState.cart,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
      };
    }
    return {
      data: null,
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    };
  },
  useQueryClient: () => ({
    invalidateQueries: mocks.invalidateQueries,
  }),
}));

vi.mock('vant/es', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vant/es')>();
  return {
    ...actual,
    showToast: mocks.showToast,
  };
});

/** enabled 既可能是布尔值，也可能是 computed ref。 */
function resolveEnabled(enabled: unknown): boolean {
  if (enabled === undefined) return true;
  if (typeof enabled === 'boolean') return enabled;
  return Boolean((enabled as { value: boolean }).value);
}

const ADDRESS = {  id: 1,
  receiver: '测试用户',
  phone: '13800000001',
  province: '上海市',
  city: '上海市',
  district: '浦东新区',
  detail: '测试路1号',
  fullAddress: '上海市上海市浦东新区测试路1号',
  longitude: 121.47,
  latitude: 31.23,
  tag: '家',
  isDefault: true,
};

const SETTLE = {
  items: [
    {
      productId: 101,
      productName: '香煎鸡胸沙拉',
      cover: '/p101.svg',
      unit: '份',
      specId: 1001,
      specName: '标准份',
      options: [],
      optionsDesc: '',
      unitPrice: 31,
      subtotal: 31,
      calories: 430,
      subtotalCalories: 430,
      quantity: 1,
      checked: true,
      available: true,
    },
  ],
  address: ADDRESS,
  freight: {
    areaId: 1,
    areaName: '浦东新区',
    distance: 3.2,
    freightAmount: 5,
    freeShipping: false,
  },
  goodsAmount: 31,
  freightAmount: 5,
  discountAmount: 0,
  payAmount: 36,
  totalCalories: 430,
};

async function mountCheckout(direct: boolean) {
  const pinia = createPinia();
  setActivePinia(pinia);
  useUserStore(pinia).setSession({
    username: 'user01',
    nickname: '测试用户',
    accessToken: 'test-token',
    expires: '2099-01-01T00:00:00Z',
    roles: ['ROLE_USER'],
  });
  if (direct) {
    useCheckoutStore(pinia).setDirectDraft({
      productId: 101,
      productName: '香煎鸡胸沙拉',
      specId: 1001,
      specName: '标准份',
      optionItemIds: [3001],
      optionsDesc: '加溏心蛋',
      quantity: 1,
    });
  }

  mocks.queryState.settle = ref(SETTLE);
  mocks.queryState.settleLoading = false;
  mocks.queryState.settleFailed = false;
  mocks.queryState.settleError = ref(null);
  mocks.queryState.addresses = ref([ADDRESS]);
  mocks.queryState.slots = ref([
    { id: 1, date: '2099-01-01', name: '午间', remainingCapacity: 10 },
  ]);
  mocks.queryState.coupons = ref([]);
  mocks.queryState.cart = ref({
    items: [],
    goodsAmount: 0,
    totalCalories: 0,
    checkedCount: 0,
  });

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/checkout',
        name: 'checkout',
        component: CheckoutPage,
      },
      { path: '/cart', name: 'cart', component: { template: '<div />' } },
      {
        path: '/product/:id',
        name: 'productDetail',
        component: { template: '<div />' },
      },
      { path: '/pay/:orderNo', name: 'pay', component: { template: '<div />' } },
      { path: '/address', name: 'address', component: { template: '<div />' } },
      { path: '/member/coupons', name: 'coupons', component: { template: '<div />' } },
    ],
  });
  await router.push(direct ? '/checkout?mode=direct' : '/checkout');
  await router.isReady();
  const wrapper = mount(CheckoutPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        'van-radio-group': { template: '<div><slot /></div>' },
        'van-radio': { template: '<div><slot /></div>' },
      },
    },
  });
  await flushPromises();
  return { wrapper, router };
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.settleCart.mockResolvedValue(SETTLE);
  mocks.createOrder.mockResolvedValue({ orderNo: 'FM20261116000001' });
  mocks.getCart.mockResolvedValue({ items: [], goodsAmount: 0, totalCalories: 0, checkedCount: 0 });
  mocks.listUserCoupons.mockResolvedValue([]);
  mocks.getAvailableDeliverySlots.mockResolvedValue([
    { id: 1, date: '2099-01-01', name: '午间', remainingCapacity: 10 },
  ]);
  mocks.invalidateQueries.mockResolvedValue(undefined);
});

describe('checkout direct purchase mode', () => {
  it('settles only the direct item and never reads the cart', async () => {
    const { wrapper } = await mountCheckout(true);

    expect(mocks.getCart).not.toHaveBeenCalled();
    expect(mocks.settleCart).toHaveBeenCalledWith(
      expect.objectContaining({
        deliveryType: 1,
        cartItemIds: undefined,
        directItems: [
          {
            productId: 101,
            specId: 1001,
            optionItemIds: [3001],
            quantity: 1,
          },
        ],
      }),
      expect.anything(),
    );
    expect(wrapper.text()).toContain('香煎鸡胸沙拉');
  });

  it('links back to the product detail page for editing in direct mode', async () => {
    const { wrapper } = await mountCheckout(true);

    const backLink = wrapper
      .findAll('a')
      .find((link) => link.text() === '返回修改');
    expect(backLink?.attributes('href')).toBe('/product/101');
  });

  it('submits the order with the direct item and clears the draft', async () => {
    const { wrapper, router } = await mountCheckout(true);
    const checkoutStore = useCheckoutStore();

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '提交订单');
    if (!submitButton) throw new Error('submit action not found');
    await submitButton.trigger('click');
    await flushPromises();

    expect(mocks.createOrder).toHaveBeenCalledWith(
      expect.objectContaining({
        directItems: [
          {
            productId: 101,
            specId: 1001,
            optionItemIds: [3001],
            quantity: 1,
          },
        ],
      }),
    );
    expect(checkoutStore.directDraft).toBeNull();
    expect(router.currentRoute.value.path).toBe('/pay/FM20261116000001');
  });

  it('keeps the cart flow unchanged outside direct mode', async () => {
    await mountCheckout(false);

    expect(mocks.getCart).toHaveBeenCalled();
    expect(mocks.settleCart).not.toHaveBeenCalledWith(
      expect.objectContaining({ directItems: expect.anything() }),
      expect.anything(),
    );
  });
});
