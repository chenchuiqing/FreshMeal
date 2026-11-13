import { defineConfig } from 'oxlint';

export default defineConfig({
  categories: {
    correctness: 'error',
    suspicious: 'warn',
  },
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  ignorePatterns: ['dist/**', 'node_modules/**'],
  rules: {
    'no-console': ['error', { allow: ['warn', 'error'] }],
    eqeqeq: ['error', 'always'],
    'prefer-const': 'error',
    'typescript/no-floating-promises': 'error',
  },
});
