package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.util.List;

public record CartLineVO(
        Long id,
        Long productId,
        String productName,
        String cover,
        String unit,
        Long specId,
        String specName,
        List<CartOptionVO> options,
        String optionsDesc,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        Integer calories,
        Integer subtotalCalories,
        Integer quantity,
        Boolean checked,
        Boolean available
) {
}
