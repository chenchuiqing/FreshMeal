import type { GlobalComponents as VantGlobalComponents } from 'vant';

declare module 'vue' {
  interface GlobalComponents extends VantGlobalComponents {}
}
