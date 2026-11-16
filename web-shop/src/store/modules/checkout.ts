import { defineStore } from 'pinia';

export interface DirectPurchaseDraft {
  productId: number;
  productName: string;
  specId?: number;
  specName?: string;
  optionItemIds: number[];
  optionsDesc: string;
  quantity: number;
}

interface CheckoutState {
  /** 立即购买草稿：结算页据此跳过购物车，直接按该商品计价。 */
  directDraft: DirectPurchaseDraft | null;
}

export const useCheckoutStore = defineStore('checkout', {
  state: (): CheckoutState => ({
    directDraft: null,
  }),
  getters: {
    hasDirectDraft: (state): boolean => state.directDraft !== null,
  },
  actions: {
    setDirectDraft(draft: DirectPurchaseDraft) {
      this.directDraft = draft;
    },
    clearDirectDraft() {
      this.directDraft = null;
    },
  },
});
