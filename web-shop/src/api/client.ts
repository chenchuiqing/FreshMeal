import axios from 'axios';
import type {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import { ApiError } from '@/types/api/common';
import type { ApiErrorResponse, ApiResponse } from '@/types/api/common';

interface ApiClientRuntime {
  getAccessToken?: () => string | undefined;
  onUnauthorized?: () => unknown;
}

const runtime: ApiClientRuntime = {};

export function configureApiClient(config: ApiClientRuntime) {
  Object.assign(runtime, config);
}

function createRequestId() {
  return (
    globalThis.crypto?.randomUUID?.() ??
    `req-${Date.now()}-${Math.random().toString(16).slice(2)}`
  );
}

function isCurrentSession(config?: InternalAxiosRequestConfig): boolean {
  const token = runtime.getAccessToken?.();
  const expected = token ? `Bearer ${token}` : null;
  return config?.headers.get('Authorization') === expected;
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    typeof value.code === 'number' &&
    Number.isInteger(value.code) &&
    'message' in value &&
    typeof value.message === 'string' &&
    'data' in value &&
    'timestamp' in value &&
    typeof value.timestamp === 'number' &&
    Number.isInteger(value.timestamp)
  );
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return isApiResponse(value) && value.data === null;
}

function notificationRequired(
  error: AxiosError<unknown>,
  normalized: ApiError,
): boolean {
  return (
    (normalized.status === 401 || normalized.code === 40_101) &&
    isCurrentSession(error.config)
  );
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (!axios.isAxiosError(error)) {
    return new ApiError(error instanceof Error ? error.message : '未知错误', {
      kind: 'unknown',
      cause: error,
    });
  }

  if (error.code === 'ERR_CANCELED') {
    return new ApiError('请求已取消', { kind: 'cancelled', cause: error });
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return new ApiError('请求超时', { kind: 'timeout', cause: error });
  }
  if (!error.response) {
    return new ApiError('网络连接失败', { kind: 'network', cause: error });
  }

  const { status, data, headers } = error.response;
  const payload = isApiErrorResponse(data) ? data : undefined;
  return new ApiError(payload?.message || error.message || `HTTP ${status}`, {
    kind: status >= 400 ? 'http' : 'business',
    code: payload?.code,
    status,
    requestId: headers['x-request-id'] as string | undefined,
    details: payload ?? data,
    cause: error,
  });
}

export const axiosInstance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  withCredentials: false,
});

axiosInstance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = runtime.getAccessToken?.();
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  if (
    import.meta.env.VITE_REQUEST_ID_ENABLED &&
    !config.headers.has('X-Request-ID')
  ) {
    config.headers.set('X-Request-ID', createRequestId());
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response: AxiosResponse<unknown>) => {
    const payload = response.data;
    if (!isApiResponse(payload)) {
      return Promise.reject(
        new ApiError('接口响应格式错误', {
          kind: 'unknown',
          status: response.status,
          requestId: response.headers['x-request-id'] as string | undefined,
          details: payload,
        }),
      );
    }

    if (payload.code !== 200) {
      const apiError = new ApiError(payload.message || '请求失败', {
        kind: response.status >= 400 ? 'http' : 'business',
        code: payload.code,
        status: response.status,
        requestId: response.headers['x-request-id'] as string | undefined,
        details: payload,
      });
      if (payload.code === 40_101 && isCurrentSession(response.config)) {
        void runtime.onUnauthorized?.();
      }
      return Promise.reject(apiError);
    }

    return response;
  },
  (error: unknown) => {
    const apiError = normalizeApiError(error);
    if (axios.isAxiosError(error) && notificationRequired(error, apiError)) {
      void runtime.onUnauthorized?.();
    }
    return Promise.reject(apiError);
  },
);

function unwrap<T>(
  request: Promise<AxiosResponse<ApiResponse<T>>>,
): Promise<T> {
  return request.then((response) => response.data.data);
}

export const apiClient = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap(axiosInstance.get<ApiResponse<T>>(url, config));
  },
  post<T, TBody = unknown>(
    url: string,
    data?: TBody,
    config?: AxiosRequestConfig<TBody>,
  ) {
    return unwrap(
      axiosInstance.post<ApiResponse<T>, AxiosResponse<ApiResponse<T>>, TBody>(
        url,
        data,
        config,
      ),
    );
  },
  put<T, TBody = unknown>(
    url: string,
    data?: TBody,
    config?: AxiosRequestConfig<TBody>,
  ) {
    return unwrap(
      axiosInstance.put<ApiResponse<T>, AxiosResponse<ApiResponse<T>>, TBody>(
        url,
        data,
        config,
      ),
    );
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap(axiosInstance.delete<ApiResponse<T>>(url, config));
  },
};
