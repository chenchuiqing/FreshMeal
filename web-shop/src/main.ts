import { createApp } from 'vue';
import App from './App.vue';
import router from '@/router';
import store from '@/store';
import { useUserStore } from '@/store/modules/user';
import { configureApiClient } from '@/api/client';
import { setupVueQuery } from '@/plugins/query';
import 'virtual:svg-icons-register';
import '@/styles/index.scss';

function bootstrap() {
  const app = createApp(App);

  app.use(store);
  const userStore = useUserStore(store);

  configureApiClient({
    getAccessToken: () => userStore.token || undefined,
    onUnauthorized: () => {
      userStore.clearSession();
      const currentRoute = router.currentRoute.value;
      if (!['login', 'register'].includes(String(currentRoute.name))) {
        void router.replace({
          name: 'login',
          query: { redirect: currentRoute.fullPath },
        });
      }
    },
  });

  setupVueQuery(app);
  app.use(router);
  app.mount('#app');
}

bootstrap();
