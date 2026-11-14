package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendVO(
        LocalDate statDate,
        BigDecimal revenue,
        long orderCount
) {
}
