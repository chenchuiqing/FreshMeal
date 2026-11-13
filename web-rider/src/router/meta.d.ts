import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    section?: 'tasks' | 'profile';
    requiresAuth?: boolean;
    guestOnly?: boolean;
    fullscreen?: boolean;
  }
}
