package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponVO(
        Long id,
        String name,
        Integer type,
        BigDecimal threshold,
        BigDecimal amount,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalCount,
        Integer receivedCount,
        Integer status
) {
}
