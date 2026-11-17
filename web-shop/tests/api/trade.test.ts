import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  addCartItem,
  cancelOrder,
  cartQueryKey,
  createAddress,
  createOrder,
  deleteCartItem,
  getCart,
  getOrderDetail,
  getOrders,
  initiatePayment,
  listAddresses,
  notifyMockPayment,
  settleCart,
  updateCartItem,
} from '@/api/modules/trade';

const transport = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));
vi.mock('@/api/client', () => ({ apiClient: transport }));

const queryClient = vi.hoisted(() => ({
  invalidateQueries: vi.fn(),
}));
vi.mock('@/plugins/query', () => ({ queryClient }));

beforeEach(() => {
  transport.get.mockReset();
  transport.post.mockReset();
  transport.put.mockReset();
  transport.delete.mockReset();
  queryClient.invalidateQueries.mockReset();
  queryClient.invalidateQueries.mockResolvedValue(undefined);
});

describe('shop trade API module', () => {
  it('routes cart, checkout and order operations consistently', async () => {
    const signal = new AbortController().signal;
    transport.get.mockResolvedValue(null);
    transport.post.mockResolvedValue(null);
    transport.put.mockResolvedValue(null);
    transport.delete.mockResolvedValue(null);

    const add = {
      productId: 101,
      specId: 1001,
      optionItemIds: [3001],
      quantity: 2,
    };
    await getCart(signal);
    await addCartItem(add, signal);
    await updateCartItem(7, { quantity: 3 }, signal);
    await deleteCartItem(7, signal);
    await settleCart(
      { addressId: 1, deliveryType: 1, cartItemIds: [7] },
      signal,
    );
    await createOrder(
      {
        addressId: 1,
        deliveryType: 1,
        cartItemIds: [7],
        remark: '少冰',
      },
      signal,
    );
    await getOrders({ status: 0, current: 1, size: 10 }, signal);
    await getOrderDetail('FM123', signal);
    await cancelOrder('FM123', signal);

    expect(transport.get).toHaveBeenNthCalledWith(1, '/shop/cart', { signal });
    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/shop/cart',
      add,
      { signal },
    );
    expect(transport.put).toHaveBeenNthCalledWith(
      1,
      '/shop/cart/7',
      { quantity: 3 },
      { signal },
    );
    expect(transport.delete).toHaveBeenNthCalledWith(
      1,
      '/shop/cart/7',
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/shop/cart/settle',
      { addressId: 1, deliveryType: 1, cartItemIds: [7] },
      { signal },
    );
    expect(transport.get).toHaveBeenNthCalledWith(
      2,
      '/shop/order/page',
      {
        params: { status: 0, current: 1, size: 10 },
        signal,
      },
    );
    expect(transport.get).toHaveBeenNthCalledWith(
      3,
      '/shop/order/FM123',
      { signal },
    );
  });

  it('keeps address and payment callbacks on contract paths', async () => {
    const signal = new AbortController().signal;
    const address = {
      receiver: '用户',
      phone: '13800000001',
      province: '上海市',
      city: '上海市',
      district: '浦东新区',
      detail: '1号',
      longitude: 121.47,
      latitude: 31.23,
      tag: '家',
      isDefault: true,
    };
    transport.get.mockResolvedValue(null);
    transport.post.mockResolvedValue(null);

    await listAddresses(signal);
    await createAddress(address, signal);
    await initiatePayment('FM123', { payType: 1 }, signal);
    await notifyMockPayment(
      { payNo: 'PAY123', payType: 1, success: true },
      signal,
    );

    expect(transport.get).toHaveBeenCalledWith('/shop/address', { signal });
    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/shop/address',
      address,
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/shop/order/FM123/pay',
      { payType: 1 },
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      3,
      '/pay/mock/notify',
      { payNo: 'PAY123', payType: 1, success: true },
      { signal },
    );
  });

  describe('cart cache invalidation', () => {
    const add = { productId: 101, specId: 1001, optionItemIds: [], quantity: 1 };

    it('invalidates the cart query after each cart write succeeds', async () => {
      transport.post.mockResolvedValue(null);
      transport.put.mockResolvedValue(null);
      transport.delete.mockResolvedValue(null);

      await addCartItem(add);
      await updateCartItem(7, { quantity: 2 });
      await deleteCartItem(7);

      expect(queryClient.invalidateQueries).toHaveBeenCalledTimes(3);
      for (const call of queryClient.invalidateQueries.mock.calls) {
        expect(call[0]).toEqual({ queryKey: cartQueryKey });
      }
    });

    it('leaves the cart query untouched when a cart write fails', async () => {
      transport.put.mockRejectedValue(new Error('conflict'));

      await expect(updateCartItem(7, { quantity: 2 })).rejects.toThrow(
        'conflict',
      );

      expect(queryClient.invalidateQueries).not.toHaveBeenCalled();
    });

    it('invalidates the cart query after a cart checkout succeeds', async () => {
      transport.post.mockResolvedValue(null);

      await createOrder({ addressId: 1, deliveryType: 1, cartItemIds: [7] });

      expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
        queryKey: cartQueryKey,
      });
    });

    it('skips cart invalidation for direct purchase orders', async () => {
      transport.post.mockResolvedValue(null);

      await createOrder({
        addressId: 1,
        deliveryType: 1,
        directItems: [
          { productId: 101, specId: 1001, optionItemIds: [], quantity: 1 },
        ],
      });

      expect(queryClient.invalidateQueries).not.toHaveBeenCalled();
    });

    it('invalidates the cart query after an order is cancelled', async () => {
      transport.delete.mockResolvedValue(null);

      await cancelOrder('FM123');

      expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
        queryKey: cartQueryKey,
      });
    });

    it('never invalidates the cart query from read-only calls', async () => {
      transport.get.mockResolvedValue(null);
      transport.post.mockResolvedValue(null);

      await getCart();
      await settleCart({ addressId: 1, deliveryType: 1, cartItemIds: [7] });

      expect(queryClient.invalidateQueries).not.toHaveBeenCalled();
    });
  });
});
