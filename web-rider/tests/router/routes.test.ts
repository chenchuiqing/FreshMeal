import { describe, expect, it } from 'vitest';
import type { RouteRecordRaw } from 'vue-router';
import routes from '@/router/routes';

function flattenRoutes(routeRecords: RouteRecordRaw[]): RouteRecordRaw[] {
  return routeRecords.flatMap((route) => [
    route,
    ...flattenRoutes(route.children ?? []),
  ]);
}

describe('rider routes', () => {
  const allRoutes = flattenRoutes(routes);

  it('requires authentication for task and profile pages', () => {
    expect(
      allRoutes.find((route) => route.name === 'tasks')?.meta?.requiresAuth,
    ).toBe(true);
    expect(
      allRoutes.find((route) => route.name === 'profile')?.meta?.requiresAuth,
    ).toBe(true);
    expect(
      allRoutes.find((route) => route.name === 'login')?.meta?.guestOnly,
    ).toBe(true);
  });

  it('does not define a registration route', () => {
    expect(allRoutes.some((route) => route.name === 'register')).toBe(false);
    expect(allRoutes.some((route) => route.path === '/register')).toBe(false);
  });
});
