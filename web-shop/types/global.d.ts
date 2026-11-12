export {};

declare global {
  export type Recordable<T = unknown> = Record<string, T>;

  interface ViteEnv {
    VITE_API_BASE_URL: string;
    VITE_API_TARGET: string;
    VITE_API_PROXY_REWRITE: boolean;
    VITE_REQUEST_ID_ENABLED: boolean;
    VITE_PORT: number;
    VITE_USE_COMPRESS: boolean;
  }

  interface ImportMetaEnv extends ViteEnv {}

  interface ImportMeta {
    readonly env: ImportMetaEnv;
  }
}
