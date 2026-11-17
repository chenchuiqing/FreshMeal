import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import type { VueWrapper } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { ref } from 'vue';
import ProductDetailPage from '@/views/product/detail.vue';
import { useCheckoutStore } from '@/store/modules/checkout';
import { useUserStore } from '@/store/modules/user';
import type { ShopProductDetail } from '@/api/modules/product';

const mocks = vi.hoisted(() => ({
  getShopProductDetail: vi.fn(),
  getFavoriteIds: vi.fn(),
  recordBrowsingHistory: vi.fn(),
  toggleFavorite: vi.fn(),
  addCartItem: vi.fn(),
  invalidateQueries: vi.fn(),
  showToast: vi.fn(),
  queryState: {
    detail: null as unknown,
    favoriteIds: [] as unknown,
    error: null as unknown,
    isLoading: null as unknown,
    isError: null as unknown,
    refetch: vi.fn(),
  },
}));

vi.mock('@/api/modules/product', () => ({
  getShopProductDetail: mocks.getShopProductDetail,
}));

vi.mock('@/api/modules/engagement', () => ({
  getFavoriteIds: mocks.getFavoriteIds,
  recordBrowsingHistory: mocks.recordBrowsingHistory,
  toggleFavorite: mocks.toggleFavorite,
}));

vi.mock('@/api/modules/trade', () => ({
  addCartItem: mocks.addCartItem,
}));

vi.mock('@tanstack/vue-query', () => ({
  useQuery: (options: { queryKey: unknown[] }) => {
    const isFavoriteQuery = options.queryKey.includes('favorite');
    return {
      data: isFavoriteQuery ? mocks.queryState.favoriteIds : mocks.queryState.detail,
      error: mocks.queryState.error,
      isLoading: mocks.queryState.isLoading,
      isError: mocks.queryState.isError,
      refetch: mocks.queryState.refetch,
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

function createDetail(
  overrides: Partial<ShopProductDetail> = {},
): ShopProductDetail {
  return {
    id: 101,
    categoryId: 11,
    categoryName: '蛋白沙拉',
    name: '香煎鸡胸沙拉',
    subtitle: '低脂高蛋白，饱腹无负担',
    cover: '/api/files/products/p101.svg',
    detail: '低温慢煎鸡胸配混合生菜。',
    productType: 1,
    price: 28,
    originalPrice: 34,
    unit: '份',
    stock: 150,
    salesCount: 320,
    status: 1,
    calories: 352,
    protein: 38.5,
    fat: 12.3,
    carbohydrate: 28.6,
    sodium: 420,
    nutritionDesc: '蛋白质 38.5g',
    allergens: ['鸡蛋'],
    storageType: 2,
    storageLabel: '0-4℃ 冷藏',
    shelfLifeDays: 3,
    producer: 'FreshMeal 中央厨房',
    heatingMethod: '开盖即食',
    healthTags: ['低卡', '高蛋白'],
    available: true,
    specs: [
      {
        id: 1001,
        name: '标准份',
        priceDelta: 0,
        stock: 150,
        calories: 352,
        isDefault: 1,
        sort: 0,
        status: 1,
      },
    ],
    options: [
      {
        id: 2001,
        name: '加料站',
        type: 2,
        required: 0,
        sort: 10,
        items: [
          {
            id: 3001,
            name: '加溏心蛋',
            priceDelta: 3,
            calories: 78,
            sort: 0,
            status: 1,
          },
        ],
      },
    ],
    images: [],
    ...overrides,
  };
}

async function mountDetail(detail = createDetail(), loggedIn = true) {
  const pinia = createPinia();
  setActivePinia(pinia);
  if (loggedIn) {
    useUserStore(pinia).setSession({
      username: 'user01',
      nickname: '测试用户',
      accessToken: 'test-token',
      expires: '2099-01-01T00:00:00Z',
      roles: ['ROLE_USER'],
    });
  }
  mocks.queryState.detail = ref(detail);
  mocks.queryState.favoriteIds = ref([]);
  mocks.queryState.error = ref(null);
  mocks.queryState.isLoading = ref(false);
  mocks.queryState.isError = ref(false);

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/product/:id', name: 'productDetail', component: ProductDetailPage },
      { path: '/checkout', name: 'checkout', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/catalog', name: 'catalog', component: { template: '<div />' } },
    ],
  });
  await router.push('/product/101');
  await router.isReady();
  const wrapper = mount(ProductDetailPage, {
    global: {
      plugins: [router, pinia],
      stubs: {
        'van-radio-group': { template: '<div><slot /></div>' },
        'van-radio': { template: '<div><slot /></div>' },
        'van-checkbox-group': { template: '<div><slot /></div>' },
        'van-checkbox': { template: '<div><slot /></div>' },
      },
    },
  });
  await flushPromises();
  return { wrapper, router };
}

function findButton(wrapper: VueWrapper, label: string) {
  const button = wrapper
    .findAll('button')
    .find((candidate) => candidate.text() === label);
  if (!button) throw new Error(`button not found: ${label}`);
  return button;
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.getFavoriteIds.mockResolvedValue([]);
  mocks.recordBrowsingHistory.mockResolvedValue(null);
  mocks.addCartItem.mockResolvedValue({ id: 1 });
  mocks.invalidateQueries.mockResolvedValue(undefined);
});

describe('product detail add to cart action', () => {
  it('submits the selected item to the cart write API', async () => {
    const { wrapper } = await mountDetail();

    await findButton(wrapper, '加入购物车').trigger('click');
    await flushPromises();

    // 缓存失效统一由 trade 模块写接口负责，页面只调用写接口（见 tests/api/trade.test.ts）。
    expect(mocks.addCartItem).toHaveBeenCalledTimes(1);
  });
});

describe('product detail buy now action', () => {
  it('exposes a buy now action next to adding to the cart', async () => {
    const { wrapper } = await mountDetail();

    expect(wrapper.text()).toContain('加入购物车');
    expect(wrapper.text()).toContain('立即购买');
  });

  it('records a direct purchase draft and routes to checkout without touching the cart', async () => {
    const { wrapper, router } = await mountDetail();
    const checkoutStore = useCheckoutStore();

    await findButton(wrapper, '立即购买').trigger('click');
    await flushPromises();

    expect(mocks.addCartItem).not.toHaveBeenCalled();
    expect(checkoutStore.directDraft).toEqual({
      productId: 101,
      productName: '香煎鸡胸沙拉',
      specId: 1001,
      specName: '标准份',
      optionItemIds: [],
      optionsDesc: '',
      quantity: 1,
    });
    expect(router.currentRoute.value.name).toBe('checkout');
    expect(router.currentRoute.value.query.mode).toBe('direct');
  });

  it('disables the buy now action when the product is unavailable', async () => {
    const { wrapper } = await mountDetail(
      createDetail({ available: false, stock: 0 }),
    );

    const button = findButton(wrapper, '立即购买');
    expect(button.attributes('disabled')).toBeDefined();
  });

  it('disables the buy now action while a required option group is unselected', async () => {
    const { wrapper } = await mountDetail(
      createDetail({
        options: [
          {
            id: 2002,
            name: '主食选择',
            type: 1,
            required: 1,
            sort: 10,
            items: [
              {
                id: 3004,
                name: '糙米饭',
                priceDelta: 0,
                calories: 0,
                sort: 0,
                status: 1,
              },
            ],
          },
        ],
      }),
    );

    const button = findButton(wrapper, '立即购买');
    expect(button.attributes('disabled')).toBeDefined();
  });

  it('sends a signed-out visitor to login before starting the purchase', async () => {
    const { wrapper, router } = await mountDetail(createDetail(), false);

    await findButton(wrapper, '立即购买').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.name).toBe('login');
    expect(router.currentRoute.value.query.redirect).toBe('/product/101');
  });
});
