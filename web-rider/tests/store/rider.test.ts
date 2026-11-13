import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useRiderStore } from '@/store/modules/rider';
import type { RiderLoginResult } from '@/api/modules/auth';
import { ApiError } from '@/types/api/common';

const auth = vi.hoisted(() => ({
  loginRider: vi.fn(),
  logoutRider: vi.fn(),
}));
vi.mock('@/api/modules/auth', () => auth);

const riderSession: RiderLoginResult = {
  accessToken: 'rider-token',
  expires: '2026-11-20T12:00:00Z',
  username: 'rider01',
  realName: 'Rider 01',
  phone: '13800000003',
  avatar: '',
  roles: ['rider'],
};

beforeEach(() => {
  setActivePinia(createPinia());
  auth.loginRider.mockReset();
  auth.logoutRider.mockReset();
  auth.logoutRider.mockResolvedValue(null);
});

describe('rider session store', () => {
  it('stores login state and clears it after logout', async () => {
    auth.loginRider.mockResolvedValue(riderSession);
    const store = useRiderStore();

    await store.login({ username: 'rider01', password: '123456' });
    expect(store.token).toBe('rider-token');
    expect(store.getRiderInfo?.username).toBe('rider01');

    await store.logout();
    expect(auth.logoutRider).toHaveBeenCalledOnce();
    expect(store.token).toBe('');
    expect(store.getRiderInfo).toBeNull();
  });

  it('clears local state even when remote logout fails', async () => {
    auth.loginRider.mockResolvedValue(riderSession);
    auth.logoutRider.mockRejectedValue(
      new ApiError('网络连接失败', { kind: 'network' }),
    );
    const store = useRiderStore();
    await store.login({ username: 'rider01', password: '123456' });

    await expect(store.logout()).rejects.toMatchObject({ kind: 'network' });
    expect(store.token).toBe('');
    expect(store.info).toBeNull();
  });

  it('rejects a login response without an access token', async () => {
    auth.loginRider.mockResolvedValue({
      ...riderSession,
      accessToken: '',
    });
    const store = useRiderStore();

    await expect(
      store.login({ username: 'rider01', password: '123456' }),
    ).rejects.toMatchObject({ message: '登录响应缺少访问令牌' });
    expect(store.token).toBe('');
  });
});
