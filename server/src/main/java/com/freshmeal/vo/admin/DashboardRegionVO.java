package com.freshmeal.vo.admin;

import java.math.BigDecimal;

public record DashboardRegionVO(
        Long areaId,
        String areaName,
        long orderCount,
        BigDecimal revenue
) {
}
