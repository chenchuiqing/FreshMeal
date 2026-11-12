import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  loginPassword,
  logoutPassword,
  registerAccount,
} from '@/api/modules/auth';

const transport = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));
vi.mock('@/api/client', () => ({ apiClient: transport }));

beforeEach(() => {
  transport.get.mockReset();
  transport.post.mockReset();
  transport.put.mockReset();
  transport.delete.mockReset();
});

describe('shop auth API module', () => {
  it('keeps login, register and logout paths inside the auth module', async () => {
    const signal = new AbortController().signal;
    const params = { username: 'user01', password: '123456' };
    transport.post.mockResolvedValue(null);

    await loginPassword(params, signal);
    await registerAccount(params, signal);
    await logoutPassword(signal);

    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/shop/auth/login',
      params,
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/shop/auth/register',
      params,
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      3,
      '/shop/auth/logout',
      undefined,
      { signal },
    );
  });

  it('preserves rejected requests for callers to present errors', async () => {
    const error = new Error('offline');
    transport.post.mockRejectedValue(error);
    await expect(
      loginPassword({ username: 'user01', password: '123456' }),
    ).rejects.toBe(error);
  });
});
