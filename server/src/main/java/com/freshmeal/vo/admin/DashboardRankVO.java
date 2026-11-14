package com.freshmeal.vo.admin;

import java.math.BigDecimal;

public record DashboardRankVO(
        Long productId,
        String productName,
        long quantity,
        BigDecimal revenue
) {
}
