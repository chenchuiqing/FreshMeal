import { apiClient } from '@/api/client';
import { queryClient } from '@/plugins/query';
import type { components } from '@/types/api/generated';

/** 购物车查询键：读取方与失效方统一引用，避免键值漂移导致缓存不失效。 */
export const cartQueryKey = ['shop', 'cart'];

/**
 * 购物车写接口成功后统一失效购物车查询。
 * 放在接口层而非页面，保证任何页面调用这些接口都不会漏掉缓存刷新。
 */
async function invalidateCartQuery() {
  // invalidateQueries 在默认配置下不会 reject，失败由各查询自行记录状态。
  await queryClient.invalidateQueries({ queryKey: cartQueryKey });
}

export type Address = components['schemas']['Address'];
export type AddressSaveRequest = components['schemas']['AddressSaveRequest'];
export type CartItemAddRequest = components['schemas']['CartItemAddRequest'];
export type CartItemUpdateRequest = components['schemas']['CartItemUpdateRequest'];
export type CartLine = components['schemas']['CartLine'];
export type CartSummary = components['schemas']['CartSummary'];
export type DirectPurchaseItem = components['schemas']['DirectPurchaseItem'];
export type DeliverySlot = components['schemas']['DeliverySlot'];
export type DeliveryTrace = components['schemas']['DeliveryTrace'];
export type Order = components['schemas']['Order'];
export type OrderCreateRequest = components['schemas']['OrderCreateRequest'];
export type OrderPage = components['schemas']['OrderPage'];
export type OrderSettle = components['schemas']['OrderSettle'];
export type OrderSettleRequest = components['schemas']['OrderSettleRequest'];
export type OrderTimeline = components['schemas']['OrderTimeline'];
export type Payment = components['schemas']['Payment'];
export type PaymentInitiateRequest =
  components['schemas']['PaymentInitiateRequest'];
export type MockPayNotifyRequest = components['schemas']['MockPayNotifyRequest'];

export interface OrderPageParams {
  current?: number;
  size?: number;
  status?: number;
  orderNo?: string;
}

export function listAddresses(signal?: AbortSignal): Promise<Address[]> {
  return apiClient.get<Address[]>('/shop/address', { signal });
}

export function createAddress(
  params: AddressSaveRequest,
  signal?: AbortSignal,
): Promise<Address> {
  return apiClient.post<Address, AddressSaveRequest>(
    '/shop/address',
    params,
    { signal },
  );
}

export function updateAddress(
  id: number,
  params: AddressSaveRequest,
  signal?: AbortSignal,
): Promise<Address> {
  return apiClient.put<Address, AddressSaveRequest>(
    `/shop/address/${id}`,
    params,
    { signal },
  );
}

export function deleteAddress(id: number, signal?: AbortSignal): Promise<null> {
  return apiClient.delete<null>(`/shop/address/${id}`, { signal });
}

export function setDefaultAddress(
  id: number,
  signal?: AbortSignal,
): Promise<Address> {
  return apiClient.put<Address, undefined>(
    `/shop/address/${id}/default`,
    undefined,
    { signal },
  );
}

export function getCart(signal?: AbortSignal): Promise<CartSummary> {
  return apiClient.get<CartSummary>('/shop/cart', { signal });
}

export async function addCartItem(
  params: CartItemAddRequest,
  signal?: AbortSignal,
): Promise<CartLine> {
  const line = await apiClient.post<CartLine, CartItemAddRequest>(
    '/shop/cart',
    params,
    { signal },
  );
  await invalidateCartQuery();
  return line;
}

export async function updateCartItem(
  id: number,
  params: CartItemUpdateRequest,
  signal?: AbortSignal,
): Promise<CartLine> {
  const line = await apiClient.put<CartLine, CartItemUpdateRequest>(
    `/shop/cart/${id}`,
    params,
    { signal },
  );
  await invalidateCartQuery();
  return line;
}

export async function deleteCartItem(
  id: number,
  signal?: AbortSignal,
): Promise<null> {
  const result = await apiClient.delete<null>(`/shop/cart/${id}`, { signal });
  await invalidateCartQuery();
  return result;
}

export function settleCart(
  params: OrderSettleRequest,
  signal?: AbortSignal,
): Promise<OrderSettle> {
  return apiClient.post<OrderSettle, OrderSettleRequest>(
    '/shop/cart/settle',
    params,
    { signal },
  );
}

export async function createOrder(
  params: OrderCreateRequest,
  signal?: AbortSignal,
): Promise<Order> {
  const order = await apiClient.post<Order, OrderCreateRequest>(
    '/shop/order',
    params,
    { signal },
  );
  // 购物车下单会删除对应购物车行；直购模式不触及购物车，无需失效。
  if (params.cartItemIds?.length) {
    await invalidateCartQuery();
  }
  return order;
}

export function getOrders(
  params: OrderPageParams,
  signal?: AbortSignal,
): Promise<OrderPage> {
  return apiClient.get<OrderPage>('/shop/order/page', {
    params,
    signal,
  });
}

export function getOrderDetail(
  orderNo: string,
  signal?: AbortSignal,
): Promise<Order> {
  return apiClient.get<Order>(`/shop/order/${orderNo}`, { signal });
}

export function getAvailableDeliverySlots(
  signal?: AbortSignal,
): Promise<DeliverySlot[]> {
  return apiClient.get<DeliverySlot[]>('/shop/delivery-slot/available', {
    signal,
  });
}

export function getOrderDeliveryTrace(
  orderNo: string,
  signal?: AbortSignal,
): Promise<DeliveryTrace[]> {
  return apiClient.get<DeliveryTrace[]>(`/shop/order/${orderNo}/trace`, {
    signal,
  });
}

export async function cancelOrder(
  orderNo: string,
  signal?: AbortSignal,
): Promise<null> {
  const result = await apiClient.delete<null>(`/shop/order/${orderNo}`, {
    signal,
  });
  // 取消订单恢复库存，购物车行的可购状态随之变化。
  await invalidateCartQuery();
  return result;
}

export function initiatePayment(
  orderNo: string,
  params: PaymentInitiateRequest,
  signal?: AbortSignal,
): Promise<Payment> {
  return apiClient.post<Payment, PaymentInitiateRequest>(
    `/shop/order/${orderNo}/pay`,
    params,
    { signal },
  );
}

export function notifyMockPayment(
  params: MockPayNotifyRequest,
  signal?: AbortSignal,
): Promise<Payment> {
  return apiClient.post<Payment, MockPayNotifyRequest>(
    '/pay/mock/notify',
    params,
    { signal },
  );
}
