import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getHealthTags,
  getHotProducts,
  getRecommendedProducts,
  getShopCategoryTree,
  getShopProductDetail,
  getShopProductPage,
} from '@/api/modules/product';
import type { ShopProductPageParams } from '@/api/modules/product';

const transport = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));
vi.mock('@/api/client', () => ({ apiClient: transport }));

beforeEach(() => {
  transport.get.mockReset();
  transport.post.mockReset();
  transport.put.mockReset();
  transport.delete.mockReset();
});

describe('shop product API module', () => {
  it('keeps category and detail paths inside the product module', async () => {
    const signal = new AbortController().signal;
    transport.get.mockResolvedValue(null);

    await getShopCategoryTree(signal);
    expect(transport.get).toHaveBeenNthCalledWith(
      1,
      '/shop/category/tree',
      { signal },
    );

    await getShopProductDetail(101, signal);
    expect(transport.get).toHaveBeenNthCalledWith(
      2,
      '/shop/product/101',
      { signal },
    );

    await getHealthTags(signal);
    expect(transport.get).toHaveBeenNthCalledWith(
      3,
      '/shop/product/health-tags',
      { signal },
    );
  });

  it('passes combined filter params to the product page query', async () => {
    const signal = new AbortController().signal;
    const params: ShopProductPageParams = {
      current: 2,
      size: 10,
      categoryId: 11,
      keyword: '鸡胸',
      minPrice: 20,
      maxPrice: 40,
      minCalories: 400,
      maxCalories: 600,
      healthTags: '低卡,高蛋白',
      sort: 'sales',
    };
    transport.get.mockResolvedValue({
      records: [],
      total: 0,
      current: 2,
      size: 10,
      pages: 0,
    });

    await getShopProductPage(params, signal);

    expect(transport.get).toHaveBeenCalledWith('/shop/product/page', {
      params,
      signal,
    });
  });

  it('sends limit only when provided for hot and recommended lists', async () => {
    const signal = new AbortController().signal;
    transport.get.mockResolvedValue([]);

    await getHotProducts(undefined, signal);
    expect(transport.get).toHaveBeenNthCalledWith(1, '/shop/product/hot', {
      params: undefined,
      signal,
    });

    await getRecommendedProducts(6, signal);
    expect(transport.get).toHaveBeenNthCalledWith(
      2,
      '/shop/product/recommend',
      { params: { limit: 6 }, signal },
    );
  });

  it('preserves rejected requests for callers to present errors', async () => {
    const error = new Error('offline');
    transport.get.mockRejectedValue(error);
    await expect(
      getShopProductPage({ keyword: '沙拉' }),
    ).rejects.toBe(error);
  });
});
