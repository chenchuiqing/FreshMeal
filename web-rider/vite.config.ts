import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const port = Number(env.VITE_PORT || '5176');
  const apiBaseUrl = env.VITE_API_BASE_URL || '/api';
  const proxy = env.VITE_API_TARGET
    ? {
        [apiBaseUrl]: {
          target: env.VITE_API_TARGET,
          changeOrigin: true,
          rewrite:
            env.VITE_API_PROXY_REWRITE === 'true'
              ? (path: string) => path.replace(new RegExp(`^${apiBaseUrl}`), '')
              : undefined,
        },
      }
    : {};

  return {
    base: '/',
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('src', import.meta.url)),
      },
    },
    server: {
      host: '127.0.0.1',
      port,
      strictPort: true,
      proxy,
    },
    plugins: [vue()],
    build: {
      target: ['es2019', 'safari15'],
      manifest: true,
    },
    css: {
      preprocessorOptions: {
        scss: {
          quietDeps: true,
        },
      },
    },
  };
});
