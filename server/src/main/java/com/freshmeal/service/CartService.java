package com.freshmeal.service;

import com.freshmeal.dto.user.CartItemAddRequest;
import com.freshmeal.dto.user.CartItemUpdateRequest;
import com.freshmeal.vo.user.CartLineVO;
import com.freshmeal.vo.user.CartSummaryVO;
import java.util.List;

public interface CartService {

    CartSummaryVO list(Long userId);

    CartLineVO add(Long userId, CartItemAddRequest request);

    CartLineVO update(Long userId, Long cartId, CartItemUpdateRequest request);

    void delete(Long userId, Long cartId);

    List<PricedCartLine> priceSelected(Long userId, List<Long> cartItemIds, boolean requireChecked);
}
