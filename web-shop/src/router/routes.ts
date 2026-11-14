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
          requiresAuth: true,
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
      {
        path: 'member/coupons',
        name: 'coupons',
        component: () => import('@/views/member/coupons.vue'),
        meta: {
          title: '我的优惠券',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'member/favorites',
        name: 'favorites',
        component: () => import('@/views/member/favorites.vue'),
        meta: {
          title: '我的收藏',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'member/history',
        name: 'history',
        component: () => import('@/views/member/history.vue'),
        meta: {
          title: '浏览历史',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'checkout',
        name: 'checkout',
        component: () => import('@/views/checkout/index.vue'),
        meta: {
          title: '结算',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'address',
        name: 'address',
        component: () => import('@/views/address/index.vue'),
        meta: {
          title: '收货地址',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/orders/index.vue'),
        meta: {
          title: '我的订单',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'orders/:orderNo',
        name: 'orderDetail',
        component: () => import('@/views/orders/detail.vue'),
        meta: {
          title: '订单详情',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'pay/:orderNo',
        name: 'pay',
        component: () => import('@/views/pay/index.vue'),
        meta: {
          title: '模拟收银台',
          keepAlive: false,
          requiresAuth: true,
        },
      },
      {
        path: 'product/:id',
        name: 'productDetail',
        component: () => import('@/views/product/detail.vue'),
        meta: {
          title: '商品详情',
          keepAlive: false,
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
