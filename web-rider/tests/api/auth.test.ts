import { beforeEach, describe, expect, it, vi } from 'vitest';
import { loginRider, logoutRider } from '@/api/modules/auth';

const transport = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));
vi.mock('@/api/client', () => ({ apiClient: transport }));

beforeEach(() => {
  transport.post.mockReset();
});

describe('rider auth API module', () => {
  it('keeps login and logout inside the rider auth namespace', async () => {
    const signal = new AbortController().signal;
    const params = { username: 'rider01', password: '123456' };
    transport.post.mockResolvedValue(null);

    await loginRider(params, signal);
    await logoutRider(signal);

    expect(transport.post).toHaveBeenNthCalledWith(
      1,
      '/rider/auth/login',
      params,
      { signal },
    );
    expect(transport.post).toHaveBeenNthCalledWith(
      2,
      '/rider/auth/logout',
      undefined,
      { signal },
    );
  });

  it('does not expose a register function', async () => {
    const authModule = await import('@/api/modules/auth');
    expect('registerRider' in authModule).toBe(false);
  });
});
