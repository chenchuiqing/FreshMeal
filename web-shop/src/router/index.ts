import { createRouter, createWebHistory } from 'vue-router';
import type { Router } from 'vue-router';
import routes from './routes';
import store from '@/store';
import { useUserStore } from '@/store/modules/user';

const router: Router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
});

function updateDocumentTitle(title?: string) {
  document.title = title ? `${title} | FreshMeal 用户端` : 'FreshMeal 用户端';
}

router.beforeEach((to) => {
  const userStore = useUserStore(store);

  if (to.meta.requiresAuth && !userStore.token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    };
  }
  if (to.meta.guestOnly && userStore.token) return { name: 'member' };

  updateDocumentTitle(
    typeof to.meta.title === 'string' ? to.meta.title : undefined,
  );
  return true;
});

export default router;
