package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserCouponVO(
        Long id,
        Long couponId,
        String name,
        Integer type,
        BigDecimal threshold,
        BigDecimal amount,
        Integer status,
        Long orderId,
        LocalDateTime receiveTime,
        LocalDateTime useTime,
        LocalDateTime expireTime
) {
}
