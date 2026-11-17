package com.freshmeal.vo.rider;

import java.math.BigDecimal;

public record RiderStatisticsVO(
        Integer todayOrderCount,
        Integer totalOrderCount,
        Integer activeOrderCount,
        BigDecimal totalIncome,
        BigDecimal todayIncome,
        BigDecimal rating,
        Integer onlineStatus,
        Integer workStatus
) {
}
