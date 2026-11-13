import { apiClient } from '@/api/client';

export interface RiderAuthRequest {
  username: string;
  password: string;
}

export interface RiderLoginResult {
  username: string;
  realName: string;
  phone: string;
  avatar: string;
  roles: string[];
  accessToken: string;
  expires: string;
}

export function loginRider(
  params: RiderAuthRequest,
  signal?: AbortSignal,
): Promise<RiderLoginResult> {
  return apiClient.post<RiderLoginResult, RiderAuthRequest>(
    '/rider/auth/login',
    params,
    { signal },
  );
}

export function logoutRider(signal?: AbortSignal): Promise<null> {
  return apiClient.post<null, undefined>('/rider/auth/logout', undefined, {
    signal,
  });
}
