import type { RouteRecordRaw } from 'vue-router';

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: { name: 'home' },
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/home/index.vue'),
        meta: {
          title: '首页',
          section: 'home',
          keepAlive: true,
          hideHeader: true,
        },
      },
      {
        path: 'catalog',
        name: 'catalog',
        component: () => import('@/views/catalog/index.vue'),
        meta: {
          title: '今日菜单',
          section: 'catalog',
          keepAlive: true,
          hideHeader: true,
        },
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/views/cart/index.vue'),
        meta: {
          title: '购物车',
          section: 'cart',
          keepAlive: true,
          hideHeader: true,
        },
      },
      {
        path: 'member',
        name: 'member',
        component: () => import('@/views/member/index.vue'),
        meta: {
          title: '我的',
          section: 'member',
          keepAlive: true,
          hideHeader: true,
        },
      },
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', guestOnly: true, fullscreen: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', guestOnly: true, fullscreen: true },
  },
  {
    path: '/offline',
    name: 'offline',
    component: () => import('@/views/system/offline.vue'),
    meta: { title: '网络不可用', fullscreen: true },
  },
  { path: '/:pathMatch(.*)*', redirect: { name: 'home' } },
];

export default routes;
