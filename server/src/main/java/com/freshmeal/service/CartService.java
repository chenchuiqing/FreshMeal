package com.freshmeal.service;

import com.freshmeal.dto.user.CartItemAddRequest;
import com.freshmeal.dto.user.CartItemUpdateRequest;
import com.freshmeal.dto.user.DirectPurchaseItem;
import com.freshmeal.vo.user.CartLineVO;
import com.freshmeal.vo.user.CartSummaryVO;
import java.util.List;

public interface CartService {

    CartSummaryVO list(Long userId);

    CartLineVO add(Long userId, CartItemAddRequest request);

    CartLineVO update(Long userId, Long cartId, CartItemUpdateRequest request);

    void delete(Long userId, Long cartId);

    List<PricedCartLine> priceSelected(Long userId, List<Long> cartItemIds, boolean requireChecked);

    /**
     * 立即购买计价：不落购物车，按当前规格与选项重新校验并定价。
     * 行项 ID 为 null，表示该行不来自购物车。
     */
    List<PricedCartLine> priceDirectPurchase(Long userId, List<DirectPurchaseItem> items);
}
