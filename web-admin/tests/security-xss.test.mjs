import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const businessPages = [
  'src/views/category/index.vue',
  'src/views/delivery/config.vue',
  'src/views/delivery/dispatch.vue',
  'src/views/delivery/index.vue',
  'src/views/delivery/rider.vue',
  'src/views/inventory/index.vue',
  'src/views/order/index.vue',
  'src/views/product/form/index.vue',
  'src/views/product/index.vue',
  'src/views/welcome/index.vue'
];

for (const page of businessPages) {
  test(`${page} does not render raw HTML`, async () => {
    const source = await readFile(new URL(`../${page}`, import.meta.url), 'utf8');
    assert.doesNotMatch(source, /\bv-html\b/);
    assert.doesNotMatch(source, /\binnerHTML\b/);
  });
}
