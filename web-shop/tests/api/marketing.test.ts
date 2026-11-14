import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getBanners,
  getNotices,
  listAvailableCoupons,
  listOrderReviews,
  listRefunds,
  listUserCoupons,
  receiveCoupon,
  requestRefund,
  submitOrderReview,
} from '@/api/modules/marketing';
import {
  getBrowsingHistoryPage,
  getFavoriteIds,
  getFavoritePage,
  recordBrowsingHistory,
  toggleFavorite,
} from '@/api/modules/engagement';

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
  transport.get.mockResolvedValue(null);
  transport.post.mockResolvedValue(null);
});

describe('shop marketing API module', () => {
  it('keeps coupon, content, review and refund contract paths', async () => {
    const signal = new AbortController().signal;

    await listAvailableCoupons(signal);
    await listUserCoupons(signal);
    await receiveCoupon(7, signal);
    await getBanners(signal);
    await getNotices(signal);
    await submitOrderReview(
      'FM12345678',
      { productId: 101, rating: 5, content: '很好', images: [] },
      signal,
    );
    await listOrderReviews('FM12345678', signal);
    await listRefunds(signal);
    await requestRefund('FM12345678', { reason: '配送问题' }, signal);

    expect(transport.get).toHaveBeenNthCalledWith(1, '/shop/coupon/available', {
      signal,
    });
    expect(transport.get).toHaveBeenNthCalledWith(2, '/shop/coupon/list', {
      signal,
    });
    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/shop/coupon/7/receive',
      undefined,
      { signal },
    );
    expect(transport.get).toHaveBeenNthCalledWith(3, '/shop/content/banner', {
      signal,
    });
    expect(transport.get).toHaveBeenNthCalledWith(4, '/shop/content/notice', {
      signal,
    });
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/shop/order/FM12345678/review',
      { productId: 101, rating: 5, content: '很好', images: [] },
      { signal },
    );
    expect(transport.get).toHaveBeenNthCalledWith(
      5,
      '/shop/order/FM12345678/review',
      { signal },
    );
    expect(transport.get).toHaveBeenNthCalledWith(6, '/shop/refund/list', {
      signal,
    });
    expect(transport.post).toHaveBeenNthCalledWith(
      3,
      '/shop/refund/FM12345678',
      { reason: '配送问题' },
      { signal },
    );
  });

  it('keeps favorite and browsing history contract paths', async () => {
    const signal = new AbortController().signal;

    await getFavoriteIds(signal);
    await getFavoritePage({ current: 1, size: 12 }, signal);
    await toggleFavorite(101, signal);
    await recordBrowsingHistory(101, signal);
    await getBrowsingHistoryPage({ current: 1, size: 12 }, signal);

    expect(transport.get).toHaveBeenNthCalledWith(1, '/shop/favorite/ids', {
      signal,
    });
    expect(transport.get).toHaveBeenNthCalledWith(2, '/shop/favorite/page', {
      params: { current: 1, size: 12 },
      signal,
    });
    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/shop/favorite/101',
      undefined,
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/shop/history/101',
      undefined,
      { signal },
    );
    expect(transport.get).toHaveBeenNthCalledWith(3, '/shop/history/page', {
      params: { current: 1, size: 12 },
      signal,
    });
  });
});
