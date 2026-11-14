import { apiClient } from '@/api/client';
import type { components } from '@/types/api/generated';

export type ShopProductPage = components['schemas']['ShopProductPage'];

export function getFavoriteIds(signal?: AbortSignal): Promise<number[]> {
  return apiClient.get<number[]>('/shop/favorite/ids', { signal });
}

export function getFavoritePage(
  params: { current?: number; size?: number },
  signal?: AbortSignal,
): Promise<ShopProductPage> {
  return apiClient.get<ShopProductPage>('/shop/favorite/page', {
    params,
    signal,
  });
}

export function toggleFavorite(
  productId: number,
  signal?: AbortSignal,
): Promise<boolean> {
  return apiClient.post<boolean>(`/shop/favorite/${productId}`, undefined, {
    signal,
  });
}

export function recordBrowsingHistory(
  productId: number,
  signal?: AbortSignal,
): Promise<null> {
  return apiClient.post<null>(`/shop/history/${productId}`, undefined, {
    signal,
  });
}

export function getBrowsingHistoryPage(
  params: { current?: number; size?: number },
  signal?: AbortSignal,
): Promise<ShopProductPage> {
  return apiClient.get<ShopProductPage>('/shop/history/page', {
    params,
    signal,
  });
}
