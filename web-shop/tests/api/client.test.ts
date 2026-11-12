import { afterEach, describe, expect, it, vi } from 'vitest';
import { AxiosError, AxiosHeaders } from 'axios';
import type { AxiosAdapter } from 'axios';
import {
  apiClient,
  axiosInstance,
  configureApiClient,
  normalizeApiError,
} from '@/api/client';
import { ApiError } from '@/types/api/common';

const originalAdapter = axiosInstance.defaults.adapter;

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter;
  configureApiClient({ getAccessToken: undefined, onUnauthorized: undefined });
  vi.unstubAllEnvs();
});

describe('freshmeal api client', () => {
  it.each([
    null,
    '<html>Proxy error</html>',
    { code: 200, data: true },
    { code: 200, message: 'ok', data: true },
    { code: '200', message: 'ok', data: true, timestamp: 1 },
    { code: 200, message: 'ok', data: true, timestamp: '1' },
  ])('rejects malformed Result envelopes: %j', async (data) => {
    axiosInstance.defaults.adapter = async (config) => ({
      data,
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders(),
      config,
    });

    await expect(apiClient.get('/test')).rejects.toMatchObject({
      name: 'ApiError',
      kind: 'unknown',
      message: '接口响应格式错误',
    });
  });

  it('injects Bearer auth and request id, then unwraps Result data', async () => {
    vi.stubEnv('VITE_REQUEST_ID_ENABLED', 'true');
    configureApiClient({ getAccessToken: () => 'token-123' });
    const adapter = vi.fn<AxiosAdapter>(async (config) => {
      expect(config.headers.get('Authorization')).toBe('Bearer token-123');
      expect(config.headers.get('X-Request-ID')).toMatch(
        /^req-|^[0-9a-f-]{36}$/i,
      );
      return {
        data: {
          code: 200,
          message: 'success',
          data: { id: 1 },
          timestamp: 1_700_000_000_000,
        },
        status: 200,
        statusText: 'OK',
        headers: new AxiosHeaders(),
        config,
      };
    });
    axiosInstance.defaults.adapter = adapter;

    await expect(apiClient.get<{ id: number }>('/test')).resolves.toEqual({
      id: 1,
    });
  });

  it('accepts only business code 200 as success', async () => {
    axiosInstance.defaults.adapter = async (config) => ({
      data: {
        code: 201,
        message: 'created',
        data: { id: 1 },
        timestamp: 1,
      },
      status: 201,
      statusText: 'Created',
      headers: new AxiosHeaders(),
      config,
    });

    await expect(apiClient.post('/test')).rejects.toMatchObject({
      kind: 'business',
      code: 201,
      message: 'created',
    });
  });

  it('preserves semantic errors and null data', async () => {
    axiosInstance.defaults.adapter = async (config) => ({
      data: {
        code: 40_000,
        message: '参数错误',
        data: null,
        timestamp: 2,
      },
      status: 400,
      statusText: 'Bad Request',
      headers: new AxiosHeaders(),
      config,
    });

    const error = await apiClient
      .post('/test')
      .catch((error_: unknown) => error_);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      kind: 'http',
      code: 40_000,
      status: 400,
      message: '参数错误',
      details: {
        code: 40_000,
        message: '参数错误',
        data: null,
        timestamp: 2,
      },
    });
  });

  it('expires the current session on HTTP 401', async () => {
    const onUnauthorized = vi.fn();
    configureApiClient({
      getAccessToken: () => 'old-token',
      onUnauthorized,
    });
    axiosInstance.defaults.adapter = async (config) => {
      throw new AxiosError('未认证', 'ERR_BAD_REQUEST', config, undefined, {
        data: {
          code: 40_101,
          message: '未认证或登录状态已失效',
          data: null,
          timestamp: 3,
        },
        status: 401,
        statusText: 'Unauthorized',
        headers: new AxiosHeaders(),
        config,
      });
    };

    await expect(apiClient.get('/private')).rejects.toMatchObject({
      status: 401,
      code: 40_101,
    });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('does not expire a new session for a previous 401', async () => {
    let token = 'old-token';
    const onUnauthorized = vi.fn();
    configureApiClient({ getAccessToken: () => token, onUnauthorized });
    let respond: (() => void) | undefined;
    axiosInstance.defaults.adapter = (config) =>
      new Promise((_resolve, reject) => {
        respond = () => {
          reject(
            new AxiosError('Expired', 'ERR_BAD_REQUEST', config, undefined, {
              data: {
                code: 40_101,
                message: 'Expired',
                data: null,
                timestamp: 4,
              },
              status: 401,
              statusText: 'Unauthorized',
              headers: new AxiosHeaders(),
              config,
            }),
          );
        };
      });

    const request = apiClient.get('/private');
    const outcome = request.catch((error: unknown) => error);
    await vi.waitFor(() => expect(respond).toBeTypeOf('function'));
    token = 'new-token';
    respond?.();
    await expect(outcome).resolves.toMatchObject({ name: 'ApiError' });
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('normalizes timeout, network and unknown failures', () => {
    expect(
      normalizeApiError(new AxiosError('late', 'ECONNABORTED')),
    ).toMatchObject({ kind: 'timeout', message: '请求超时' });
    expect(normalizeApiError(new AxiosError('offline'))).toMatchObject({
      kind: 'network',
      message: '网络连接失败',
    });
    expect(normalizeApiError('bad')).toBeInstanceOf(ApiError);
  });
});
