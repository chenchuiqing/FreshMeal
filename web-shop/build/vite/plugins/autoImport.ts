import AutoImport from 'unplugin-auto-import/vite';
import { VantResolver } from '@vant/auto-import-resolver';

export const ConfigAutoImportPlugin = (writeDeclarations = true) =>
  AutoImport({
    dts: writeDeclarations && 'types/auto-imports.d.ts',
    imports: ['vue', 'pinia', 'vue-router'],
    eslintrc: {
      enabled: writeDeclarations,
    },
    resolvers: [VantResolver()],
  });
