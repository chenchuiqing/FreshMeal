package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminDeliveryAreaVO(
        Long id,
        String name,
        String city,
        String district,
        BigDecimal baseFee,
        BigDecimal perKmFee,
        BigDecimal freeThreshold,
        BigDecimal nightSurcharge,
        Integer sort,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
