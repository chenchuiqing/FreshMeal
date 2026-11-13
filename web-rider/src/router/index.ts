import { createRouter, createWebHistory } from 'vue-router';
import type { Router } from 'vue-router';
import routes from './routes';
import store from '@/store';
import { useRiderStore } from '@/store/modules/rider';

const router: Router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
});

function updateDocumentTitle(title?: string) {
  document.title = title ? `${title} | FreshMeal 配送端` : 'FreshMeal 配送端';
}

router.beforeEach((to) => {
  const riderStore = useRiderStore(store);

  if (to.meta.requiresAuth && !riderStore.token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    };
  }
  if (to.meta.guestOnly && riderStore.token) return { name: 'tasks' };

  updateDocumentTitle(
    typeof to.meta.title === 'string' ? to.meta.title : undefined,
  );
  return true;
});

export default router;
