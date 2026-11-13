import { describe, expect, it } from 'vitest';
import type { RouteRecordRaw } from 'vue-router';
import routes from '@/router/routes';

function flattenRoutes(routeRecords: RouteRecordRaw[]): RouteRecordRaw[] {
  return routeRecords.flatMap((route) => [
    route,
    ...flattenRoutes(route.children ?? []),
  ]);
}

describe('shop routes', () => {
  const allRoutes = flattenRoutes(routes);

  it('configures primary tabs and authentication pages', () => {
    expect(allRoutes.map((route) => route.path)).toEqual(
      expect.arrayContaining([
        '/',
        'home',
        'catalog',
        'cart',
        'member',
        'product/:id',
        '/login',
        '/register',
        '/offline',
      ]),
    );
  });

  it('keeps the product detail route lazy with a visible back header', () => {
    const route = allRoutes.find((item) => item.name === 'productDetail');
    expect(route?.path).toBe('product/:id');
    expect(route?.meta?.title).toBe('商品详情');
    expect(route?.meta?.keepAlive).toBe(false);
    expect(route?.meta?.hideHeader).toBeUndefined();
  });

  it('keeps guest pages public and redirects unknown routes home', () => {
    expect(
      allRoutes.find((route) => route.name === 'login')?.meta?.guestOnly,
    ).toBe(true);
    expect(
      allRoutes.find((route) => route.name === 'register')?.meta?.guestOnly,
    ).toBe(true);
    expect(
      allRoutes.find((route) => route.path === '/:pathMatch(.*)*')?.redirect,
    ).toEqual({ name: 'home' });
  });
});
