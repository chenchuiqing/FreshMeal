import { defineConfig } from 'vitest/config';
import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { ConfigAutoImportPlugin } from './build/vite/plugins/autoImport.ts';
import { ConfigAutoComponentsPlugin } from './build/vite/plugins/component.ts';

export default defineConfig({
  plugins: [
    vue(),
    ConfigAutoComponentsPlugin(false),
    ConfigAutoImportPlugin(false),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('src', import.meta.url)),
      '#': fileURLToPath(new URL('types', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['tests/**/*.test.ts'],
    setupFiles: ['tests/setup.ts'],
    css: {
      include: [/\.css$/],
    },
    server: {
      deps: {
        inline: ['vant'],
      },
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/api/**/*.ts', 'src/store/**/*.ts', 'src/utils/**/*.ts'],
      exclude: ['src/types/**', 'src/types/api/generated.d.ts'],
      thresholds: { lines: 60, functions: 60, statements: 60, branches: 50 },
    },
  },
});
