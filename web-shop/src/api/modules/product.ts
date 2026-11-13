import { apiClient } from '@/api/client';
import type { components } from '@/types/api/generated';

export type CategoryNode = components['schemas']['CategoryNode'];
export type ShopProduct = components['schemas']['ShopProduct'];
export type ShopProductDetail = components['schemas']['ShopProductDetail'];
export type ShopProductPage = components['schemas']['ShopProductPage'];
export type ProductSpec = components['schemas']['ProductSpec'];
export type ProductOption = components['schemas']['ProductOption'];
export type ProductOptionItem = components['schemas']['ProductOptionItem'];
export type ProductImage = components['schemas']['ProductImage'];

export type ShopProductSort = 'default' | 'sales' | 'priceAsc' | 'priceDesc';

export interface ShopProductPageParams {
  current?: number;
  size?: number;
  categoryId?: number;
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  minCalories?: number;
  maxCalories?: number;
  /** 逗号分隔的健康标签，多标签取交集。 */
  healthTags?: string;
  sort?: ShopProductSort;
}

export function getShopCategoryTree(signal?: AbortSignal): Promise<CategoryNode[]> {
  return apiClient.get<CategoryNode[]>('/shop/category/tree', { signal });
}

export function getShopProductPage(
  params: ShopProductPageParams,
  signal?: AbortSignal,
): Promise<ShopProductPage> {
  return apiClient.get<ShopProductPage>('/shop/product/page', {
    params,
    signal,
  });
}

export function getShopProductDetail(
  id: number,
  signal?: AbortSignal,
): Promise<ShopProductDetail> {
  return apiClient.get<ShopProductDetail>(`/shop/product/${id}`, { signal });
}

export function getHotProducts(
  limit?: number,
  signal?: AbortSignal,
): Promise<ShopProduct[]> {
  return apiClient.get<ShopProduct[]>('/shop/product/hot', {
    params: limit === undefined ? undefined : { limit },
    signal,
  });
}

export function getRecommendedProducts(
  limit?: number,
  signal?: AbortSignal,
): Promise<ShopProduct[]> {
  return apiClient.get<ShopProduct[]>('/shop/product/recommend', {
    params: limit === undefined ? undefined : { limit },
    signal,
  });
}

export function getHealthTags(signal?: AbortSignal): Promise<string[]> {
  return apiClient.get<string[]>('/shop/product/health-tags', { signal });
}
