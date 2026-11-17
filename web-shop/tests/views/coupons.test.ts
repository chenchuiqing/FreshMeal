import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { ref } from 'vue';
import CouponsPage from '@/views/member/coupons.vue';

const mocks = vi.hoisted(() => ({
  listAvailableCoupons: vi.fn(),
  listUserCoupons: vi.fn(),
  receiveCoupon: vi.fn(),
  showToast: vi.fn(),
  invalidateQueries: vi.fn(),
  queryState: {
    available: null as unknown,
    mine: null as unknown,
  },
}));

vi.mock('@/api/modules/marketing', () => ({
  listAvailableCoupons: mocks.listAvailableCoupons,
  listUserCoupons: mocks.listUserCoupons,
  receiveCoupon: mocks.receiveCoupon,
}));

vi.mock('@tanstack/vue-query', () => ({
  useQuery: (options: { queryKey: unknown[]; queryFn?: unknown }) => {
    const key = options.queryKey.join('|');
    if (options.queryFn) {
      void (options.queryFn as (context: { signal: AbortSignal }) => unknown)({
        signal: new AbortController().signal,
      });
    }
    if (key.includes('available')) {
      return {
        data: mocks.queryState.available,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
      };
    }
    if (key.includes('mine')) {
      return {
        data: mocks.queryState.mine,
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

const AVAILABLE = [
  {
    id: 1,
    name: '新客满减券',
    type: 1,
    threshold: 30,
    amount: 5,
    startTime: '2026-01-01T00:00:00Z',
    endTime: '2099-01-01T00:00:00Z',
    totalCount: 1000,
    receivedCount: 10,
    status: 1,
  },
  {
    id: 2,
    name: '周末八折券',
    type: 2,
    threshold: 50,
    amount: 0.8,
    startTime: '2026-01-01T00:00:00Z',
    endTime: '2099-01-01T00:00:00Z',
    totalCount: 500,
    receivedCount: 500,
    status: 1,
  },
];

async function mountPage() {
  const pinia = createPinia();
  setActivePinia(pinia);

  mocks.queryState.available = ref(AVAILABLE);
  mocks.queryState.mine = ref([]);

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/member/coupons', name: 'coupons', component: CouponsPage },
      { path: '/member', name: 'member', component: { template: '<div />' } },
    ],
  });
  await router.push('/member/coupons');
  await router.isReady();
  const wrapper = mount(CouponsPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        'van-button': {
          props: ['disabled', 'loading'],
          template:
            '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  });
  await flushPromises();
  return wrapper;
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.listAvailableCoupons.mockResolvedValue(AVAILABLE);
  mocks.listUserCoupons.mockResolvedValue([]);
  mocks.receiveCoupon.mockResolvedValue({
    id: 99,
    couponId: 1,
    name: '新客满减券',
    type: 1,
    threshold: 30,
    amount: 5,
    status: 0,
    receiveTime: '2026-01-01T00:00:00Z',
    expireTime: '2099-01-01T00:00:00Z',
  });
});

describe('优惠券页领取按钮', () => {
  it('库存领完的券按钮置灰且文案为已领完', async () => {
    const wrapper = await mountPage();
    const buttons = wrapper.findAll('button');
    const soldOut = buttons.find((button) => button.text() === '已领完');
    expect(soldOut).toBeDefined();
    expect(soldOut?.attributes('disabled')).toBeDefined();
  });

  it('可领取的券按钮可点击并调用领取接口', async () => {
    const wrapper = await mountPage();
    const buttons = wrapper.findAll('button');
    const claimable = buttons.find((button) => button.text() === '领取');
    expect(claimable).toBeDefined();
    expect(claimable?.attributes('disabled')).toBeUndefined();

    await claimable?.trigger('click');
    await flushPromises();

    expect(mocks.receiveCoupon).toHaveBeenCalledWith(1);
    expect(mocks.showToast).toHaveBeenCalledWith({
      message: '领取成功',
      icon: 'success',
    });
  });
});
