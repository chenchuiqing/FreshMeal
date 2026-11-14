import { apiClient } from '@/api/client';
import type { components } from '@/types/api/generated';

export type Address = components['schemas']['Address'];
export type AddressSaveRequest = components['schemas']['AddressSaveRequest'];
export type CartItemAddRequest = components['schemas']['CartItemAddRequest'];
export type CartItemUpdateRequest = components['schemas']['CartItemUpdateRequest'];
export type CartLine = components['schemas']['CartLine'];
export type CartSummary = components['schemas']['CartSummary'];
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

export function addCartItem(
  params: CartItemAddRequest,
  signal?: AbortSignal,
): Promise<CartLine> {
  return apiClient.post<CartLine, CartItemAddRequest>('/shop/cart', params, {
    signal,
  });
}

export function updateCartItem(
  id: number,
  params: CartItemUpdateRequest,
  signal?: AbortSignal,
): Promise<CartLine> {
  return apiClient.put<CartLine, CartItemUpdateRequest>(
    `/shop/cart/${id}`,
    params,
    { signal },
  );
}

export function deleteCartItem(id: number, signal?: AbortSignal): Promise<null> {
  return apiClient.delete<null>(`/shop/cart/${id}`, { signal });
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

export function createOrder(
  params: OrderCreateRequest,
  signal?: AbortSignal,
): Promise<Order> {
  return apiClient.post<Order, OrderCreateRequest>('/shop/order', params, {
    signal,
  });
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

export function cancelOrder(
  orderNo: string,
  signal?: AbortSignal,
): Promise<null> {
  return apiClient.delete<null>(`/shop/order/${orderNo}`, { signal });
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
