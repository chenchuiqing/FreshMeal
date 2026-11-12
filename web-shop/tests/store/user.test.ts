import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useUserStore } from '@/store/modules/user';
import type { ShopLoginResult } from '@/api/modules/auth';
import { ApiError } from '@/types/api/common';

const auth = vi.hoisted(() => ({
  loginPassword: vi.fn(),
  registerAccount: vi.fn(),
  logoutPassword: vi.fn(),
}));
vi.mock('@/api/modules/auth', () => auth);

const tokenA: ShopLoginResult = {
  accessToken: 'token-a',
  expires: '2026-11-19T12:00:00Z',
  username: 'user01',
  nickname: 'Fresh User',
  roles: ['user'],
};

beforeEach(() => {
  setActivePinia(createPinia());
  auth.loginPassword.mockReset();
  auth.registerAccount.mockReset();
  auth.logoutPassword.mockReset();
  auth.logoutPassword.mockResolvedValue(null);
});

describe('user session store', () => {
  it('stores typed login state and clears it after logout', async () => {
    auth.loginPassword.mockResolvedValue(tokenA);
    const store = useUserStore();

    await store.login({ username: 'user01', password: '123456' });
    expect(store.token).toBe('token-a');
    expect(store.getUserInfo?.username).toBe('user01');

    await store.logout();
    expect(auth.logoutPassword).toHaveBeenCalledOnce();
    expect(store.token).toBe('');
    expect(store.getUserInfo).toBeNull();
  });

  it('cancels and ignores a late login response', async () => {
    let resolveLogin: ((value: ShopLoginResult) => void) | undefined;
    let observedSignal: AbortSignal | undefined;
    auth.loginPassword.mockImplementationOnce(
      (_params, signal: AbortSignal) => {
        observedSignal = signal;
        return new Promise<ShopLoginResult>((resolve) => {
          resolveLogin = resolve;
        });
      },
    );
    const store = useUserStore();
    const pending = store.login({ username: 'user01', password: '123456' });
    const outcome = pending.catch((error: unknown) => error);

    store.clearSession();
    resolveLogin?.(tokenA);

    await expect(outcome).resolves.toMatchObject({ kind: 'cancelled' });
    expect(observedSignal?.aborted).toBe(true);
    expect(store.token).toBe('');
  });

  it('clears local state even when remote logout fails', async () => {
    auth.loginPassword.mockResolvedValue(tokenA);
    auth.logoutPassword.mockRejectedValue(
      new ApiError('网络连接失败', { kind: 'network' }),
    );
    const store = useUserStore();
    await store.login({ username: 'user01', password: '123456' });

    await expect(store.logout()).rejects.toMatchObject({ kind: 'network' });
    expect(store.token).toBe('');
    expect(store.info).toBeNull();
  });

  it('rejects a login response without an access token', async () => {
    auth.loginPassword.mockResolvedValue({
      accessToken: '',
      expires: '2026-11-19T12:00:00Z',
    });
    const store = useUserStore();

    await expect(
      store.login({ username: 'user01', password: '123456' }),
    ).rejects.toMatchObject({ message: '登录响应缺少访问令牌' });
    expect(store.token).toBe('');
  });
});
