package com.freshmeal.vo.admin;

import java.math.BigDecimal;

public record DashboardOverviewVO(
        BigDecimal todayRevenue,
        long todayOrderCount,
        long todayUserCount,
        long pendingDeliveryCount,
        long finishedTodayCount
) {
}
