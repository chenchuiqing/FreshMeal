import type { PluginOption } from 'vite';
import vue from '@vitejs/plugin-vue';
import { ConfigAutoComponentsPlugin } from './component.ts';
import { ConfigAutoImportPlugin } from './autoImport.ts';
import { ConfigCompressPlugin } from './compress.ts';
import { ConfigProgressPlugin } from './progress.ts';
import { ConfigSvgIconsPlugin } from './svgIcons.ts';

export function createVitePlugins(env: ViteEnv, isBuild: boolean) {
  const plugins: (PluginOption | PluginOption[])[] = [
    vue(),
    ConfigSvgIconsPlugin(),
    ConfigAutoComponentsPlugin(),
    ConfigAutoImportPlugin(),
    ConfigProgressPlugin(),
  ];

  if (isBuild && env.VITE_USE_COMPRESS) {
    plugins.push(ConfigCompressPlugin());
  }

  return plugins;
}
