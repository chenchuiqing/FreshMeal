import Components from 'unplugin-vue-components/vite';
import { VantResolver } from '@vant/auto-import-resolver';
import type { PluginOption } from 'vite';

export const ConfigAutoComponentsPlugin = (
  writeDeclarations = true,
): PluginOption =>
  Components({
    dirs: ['src/components'],
    extensions: ['vue'],
    deep: true,
    dts: writeDeclarations && 'types/components.d.ts',
    directoryAsNamespace: false,
    globalNamespaces: [],
    directives: true,
    include: [/\.vue$/, /\.vue\?vue/],
    exclude: [/[\\/]node_modules[\\/]/, /[\\/]\.git[\\/]/],
    resolvers: [VantResolver()],
  });
