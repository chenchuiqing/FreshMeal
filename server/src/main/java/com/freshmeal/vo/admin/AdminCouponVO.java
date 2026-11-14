package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminCouponVO(
        Long id,
        String name,
        Integer type,
        BigDecimal threshold,
        BigDecimal amount,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalCount,
        Integer receivedCount,
        Integer usedCount,
        Integer status
) {
}
