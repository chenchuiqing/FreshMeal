import { loginRider, logoutRider } from '@/api/modules/auth';
import type { RiderAuthRequest, RiderLoginResult } from '@/api/modules/auth';
import { defineStore } from 'pinia';
import { ApiError } from '@/types/api/common';

const pendingLogins = new WeakMap<object, AbortController>();

interface RiderState {
  token: string;
  info: RiderLoginResult | null;
}

function readAccessToken(result: RiderLoginResult | null): string {
  if (!result?.accessToken) {
    throw new ApiError('登录响应缺少访问令牌', { kind: 'unknown' });
  }
  return result.accessToken;
}

export const useRiderStore = defineStore('rider', {
  state: (): RiderState => ({
    token: '',
    info: null,
  }),
  getters: {
    getRiderInfo(): RiderLoginResult | null {
      return this.info;
    },
  },
  actions: {
    setSession(result: RiderLoginResult) {
      this.token = readAccessToken(result);
      this.info = result;
    },
    async login(params: RiderAuthRequest): Promise<RiderLoginResult> {
      this.cancelLogin();
      const controller = new AbortController();
      pendingLogins.set(this, controller);
      try {
        const result = await loginRider(params, controller.signal);
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
        if (this.token) await logoutRider();
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
