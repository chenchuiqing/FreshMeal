import {
  loginPassword,
  logoutPassword,
  registerAccount,
} from '@/api/modules/auth';
import type { ShopAuthRequest, ShopLoginResult } from '@/api/modules/auth';
import { defineStore } from 'pinia';
import { ApiError } from '@/types/api/common';

const pendingLogins = new WeakMap<object, AbortController>();

interface UserState {
  token: string;
  info: ShopLoginResult | null;
}

function readAccessToken(result: ShopLoginResult | null): string {
  if (!result?.accessToken) {
    throw new ApiError('登录响应缺少访问令牌', { kind: 'unknown' });
  }
  return result.accessToken;
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    info: null,
  }),
  getters: {
    getUserInfo(): ShopLoginResult | null {
      return this.info;
    },
  },
  actions: {
    setSession(result: ShopLoginResult) {
      this.token = readAccessToken(result);
      this.info = result;
    },
    async login(params: ShopAuthRequest): Promise<ShopLoginResult> {
      this.cancelLogin();
      const controller = new AbortController();
      pendingLogins.set(this, controller);
      try {
        const result = await loginPassword(params, controller.signal);
        if (controller.signal.aborted) {
          throw new ApiError('登录请求已取消', { kind: 'cancelled' });
        }
        this.setSession(result);
        return result;
      } finally {
        if (pendingLogins.get(this) === controller) {
          pendingLogins.delete(this);
        }
      }
    },
    async register(params: ShopAuthRequest): Promise<ShopLoginResult> {
      const result = await registerAccount(params);
      this.setSession(result);
      return result;
    },
    cancelLogin() {
      pendingLogins.get(this)?.abort();
      pendingLogins.delete(this);
    },
    clearSession() {
      this.cancelLogin();
      this.$reset();
    },
    async logout() {
      this.cancelLogin();
      try {
        if (this.token) await logoutPassword();
      } finally {
        this.clearSession();
      }
    },
  },
  persist: {
    pick: ['token', 'info'],
    storage: sessionStorage,
  },
});
