package com.freshmeal.vo.user;

import java.math.BigDecimal;

public record CartOptionVO(
        Long groupId,
        Long itemId,
        String name,
        BigDecimal priceDelta,
        Integer calories
) {
}
