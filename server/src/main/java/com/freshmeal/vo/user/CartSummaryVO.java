package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryVO(
        List<CartLineVO> items,
        BigDecimal goodsAmount,
        Integer totalCalories,
        Integer checkedCount
) {
}
