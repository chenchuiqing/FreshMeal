import { apiClient } from '@/api/client';
import type { components } from '@/types/api/generated';

export type ShopAuthRequest = components['schemas']['ShopAuthRequest'];
export type ShopLoginResult = components['schemas']['ShopLoginResult'];

export function loginPassword(
  params: ShopAuthRequest,
  signal?: AbortSignal,
): Promise<ShopLoginResult> {
  return apiClient.post<ShopLoginResult, ShopAuthRequest>(
    '/shop/auth/login',
    params,
    { signal },
  );
}

export function registerAccount(
  params: ShopAuthRequest,
  signal?: AbortSignal,
): Promise<ShopLoginResult> {
  return apiClient.post<ShopLoginResult, ShopAuthRequest>(
    '/shop/auth/register',
    params,
    { signal },
  );
}

export function logoutPassword(signal?: AbortSignal): Promise<null> {
  return apiClient.post<null, undefined>('/shop/auth/logout', undefined, {
    signal,
  });
}
