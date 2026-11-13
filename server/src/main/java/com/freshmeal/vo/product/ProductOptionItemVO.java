package com.freshmeal.vo.product;

import java.math.BigDecimal;

public record ProductOptionItemVO(
        Long id,
        String name,
        BigDecimal priceDelta,
        Integer calories,
        Integer sort,
        Integer status
) {
}
