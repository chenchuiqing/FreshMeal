import { createVitePlugins } from './build/vite/plugins/index.ts';
import { createViteProxy } from './build/vite/proxy.ts';
import type { ConfigEnv, UserConfig } from 'vite';
import { loadEnv } from 'vite';
import { wrapperEnv } from './build/utils.ts';
import { fileURLToPath, URL } from 'node:url';

export default function defineViteConfig({
  command,
  mode,
}: ConfigEnv): UserConfig {
  const root = process.cwd();
  const fileEnv = loadEnv(mode, root, '');
  const processEnv = Object.fromEntries(
    Object.entries(process.env).filter(
      (entry): entry is [string, string] =>
        entry[0].startsWith('VITE_') && typeof entry[1] === 'string',
    ),
  );
  const viteEnv = wrapperEnv({ ...fileEnv, ...processEnv });

  return {
    base: '/',
    root,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('src', import.meta.url)),
        '#': fileURLToPath(new URL('types', import.meta.url)),
      },
    },
    server: {
      host: '127.0.0.1',
      port: viteEnv.VITE_PORT,
      strictPort: true,
      hmr: true,
      proxy: createViteProxy(viteEnv),
      forwardConsole: true,
    },
    plugins: createVitePlugins(viteEnv, command === 'build'),
    build: {
      target: ['es2019', 'safari15'],
      manifest: true,
      rolldownOptions: {
        output: {
          minify: {
            compress: { dropConsole: true, dropDebugger: true },
          },
        },
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "@/styles/variable.scss" as *;',
          quietDeps: true,
        },
      },
    },
  };
}
