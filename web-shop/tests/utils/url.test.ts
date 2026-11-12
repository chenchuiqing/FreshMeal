import { describe, expect, it } from 'vitest';
import { getSafeRedirect, isSafeInternalPath } from '@/utils/url';

describe('safe redirects', () => {
  it('accepts an internal route', () =>
    expect(getSafeRedirect('/catalog')).toBe('/catalog'));

  it.each([
    'https://evil.test',
    '//evil.test',
    String.raw`/\evil.test`,
    1,
    null,
  ])('rejects unsafe redirect %s', (value) => {
    expect(isSafeInternalPath(value)).toBe(false);
    expect(getSafeRedirect(value)).toBe('/home');
  });
});
