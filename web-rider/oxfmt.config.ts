import { defineConfig } from 'oxfmt';

export default defineConfig({
  printWidth: 80,
  tabWidth: 2,
  proseWrap: 'never',
  semi: true,
  singleQuote: true,
  trailingComma: 'all',
  endOfLine: 'lf',
  insertFinalNewline: true,
  overrides: [
    {
      files: ['**/*.json', '**/*.jsonc'],
      options: {
        trailingComma: 'none',
        quoteProps: 'preserve',
        singleQuote: false,
      },
    },
  ],
});
