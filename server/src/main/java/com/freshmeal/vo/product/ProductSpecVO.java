package com.freshmeal.vo.product;

import java.math.BigDecimal;

public record ProductSpecVO(
        Long id,
        String name,
        BigDecimal priceDelta,
        Integer stock,
        Integer calories,
        Integer isDefault,
        Integer sort,
        Integer status
) {
}
