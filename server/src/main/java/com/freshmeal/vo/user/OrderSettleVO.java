package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.util.List;

public record OrderSettleVO(
        List<CartLineVO> items,
        AddressVO address,
        FreightVO freight,
        BigDecimal goodsAmount,
        BigDecimal freightAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        Integer totalCalories
) {
}
