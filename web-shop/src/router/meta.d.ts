import 'vue-router';

export {};

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    section?: 'home' | 'catalog' | 'cart' | 'member';
    keepAlive?: boolean;
    requiresAuth?: boolean;
    guestOnly?: boolean;
    hideHeader?: boolean;
    fullscreen?: boolean;
  }
}
