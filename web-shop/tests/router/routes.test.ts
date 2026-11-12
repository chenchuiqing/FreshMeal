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
        '/login',
        '/register',
        '/offline',
      ]),
    );
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
