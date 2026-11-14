import { apiClient } from '@/api/client';
import type { components } from '@/types/api/generated';

export type Coupon = components['schemas']['Coupon'];
export type UserCoupon = components['schemas']['UserCoupon'];
export type Review = components['schemas']['Review'];
export type ReviewSubmitRequest = components['schemas']['ReviewSubmitRequest'];
export type Banner = components['schemas']['Banner'];
export type Notice = components['schemas']['Notice'];
export type Refund = components['schemas']['Refund'];
export type RefundRequest = components['schemas']['RefundRequest'];

export function listAvailableCoupons(signal?: AbortSignal): Promise<Coupon[]> {
  return apiClient.get<Coupon[]>('/shop/coupon/available', { signal });
}

export function listUserCoupons(signal?: AbortSignal): Promise<UserCoupon[]> {
  return apiClient.get<UserCoupon[]>('/shop/coupon/list', { signal });
}

export function receiveCoupon(
  id: number,
  signal?: AbortSignal,
): Promise<UserCoupon> {
  return apiClient.post<UserCoupon>(`/shop/coupon/${id}/receive`, undefined, {
    signal,
  });
}

export function getBanners(signal?: AbortSignal): Promise<Banner[]> {
  return apiClient.get<Banner[]>('/shop/content/banner', { signal });
}

export function getNotices(signal?: AbortSignal): Promise<Notice[]> {
  return apiClient.get<Notice[]>('/shop/content/notice', { signal });
}

export function submitOrderReview(
  orderNo: string,
  params: ReviewSubmitRequest,
  signal?: AbortSignal,
): Promise<Review> {
  return apiClient.post<Review, ReviewSubmitRequest>(
    `/shop/order/${orderNo}/review`,
    params,
    { signal },
  );
}

export function listOrderReviews(
  orderNo: string,
  signal?: AbortSignal,
): Promise<Review[]> {
  return apiClient.get<Review[]>(`/shop/order/${orderNo}/review`, { signal });
}

export function listRefunds(signal?: AbortSignal): Promise<Refund[]> {
  return apiClient.get<Refund[]>('/shop/refund/list', { signal });
}

export function requestRefund(
  orderNo: string,
  params: RefundRequest,
  signal?: AbortSignal,
): Promise<null> {
  return apiClient.post<null, RefundRequest>(
    `/shop/refund/${orderNo}`,
    params,
    { signal },
  );
}
