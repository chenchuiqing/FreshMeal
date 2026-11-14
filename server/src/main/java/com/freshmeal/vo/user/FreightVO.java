package com.freshmeal.vo.user;

import java.math.BigDecimal;

public record FreightVO(
        Long areaId,
        String areaName,
        BigDecimal distance,
        BigDecimal freightAmount,
        Boolean freeShipping
) {
}
