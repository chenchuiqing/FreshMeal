package com.freshmeal.vo.user;

import java.math.BigDecimal;

public record OrderItemVO(
        Long productId,
        String productName,
        String productCover,
        String specName,
        String optionsDesc,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal,
        Integer calories,
        Integer subtotalCalories
) {
}
