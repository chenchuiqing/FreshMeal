package com.freshmeal.vo.admin;

import java.math.BigDecimal;

public record AdminOrderItemVO(
        Long productId,
        String productName,
        String productCover,
        String specName,
        String optionsDesc,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal,
        Integer calories
) {
}
