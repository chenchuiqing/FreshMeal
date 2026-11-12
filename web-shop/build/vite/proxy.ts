import type { ProxyOptions } from 'vite';

export function createViteProxy(env: ViteEnv): Record<string, ProxyOptions> {
  const proxy: Record<string, ProxyOptions> = {};
  const apiBaseUrl = env.VITE_API_BASE_URL || '/api';

  if (env.VITE_API_TARGET) {
    proxy[apiBaseUrl] = {
      target: env.VITE_API_TARGET,
      changeOrigin: true,
      rewrite: env.VITE_API_PROXY_REWRITE
        ? (path) => path.replace(new RegExp(`^${apiBaseUrl}`), '')
        : undefined,
    };
  }

  return proxy;
}
