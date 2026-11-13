import { Button, Cell, CellGroup, Field, Form, Icon } from 'vant';
import 'vant/lib/index.css';
import { createApp } from 'vue';
import App from './App.vue';
import { configureApiClient } from '@/api/client';
import router from '@/router';
import store from '@/store';
import { useRiderStore } from '@/store/modules/rider';
import '@/styles/index.scss';

function bootstrap() {
  const app = createApp(App);

  app.use(store);
  const riderStore = useRiderStore(store);

  configureApiClient({
    getAccessToken: () => riderStore.token || undefined,
    onUnauthorized: () => {
      riderStore.clearSession();
      const currentRoute = router.currentRoute.value;
      if (currentRoute.name !== 'login') {
        void router.replace({
          name: 'login',
          query: { redirect: currentRoute.fullPath },
        });
      }
    },
  });

  app.use(Form);
  app.use(Field);
  app.use(Button);
  app.use(CellGroup);
  app.use(Cell);
  app.use(Icon);
  app.use(router);
  app.mount('#app');
}

bootstrap();
