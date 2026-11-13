import type { RouteRecordRaw } from 'vue-router';

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: { name: 'tasks' },
    children: [
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('@/views/tasks/index.vue'),
        meta: {
          title: '任务大厅',
          section: 'tasks',
          requiresAuth: true,
        },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: {
          title: '我的',
          section: 'profile',
          requiresAuth: true,
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
  { path: '/:pathMatch(.*)*', redirect: { name: 'tasks' } },
];

export default routes;
