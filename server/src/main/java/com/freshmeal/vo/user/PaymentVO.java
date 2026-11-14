package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentVO(
        Long id,
        String payNo,
        String orderNo,
        Integer payType,
        Integer status,
        BigDecimal amount,
        LocalDateTime expireTime,
        LocalDateTime payTime,
        String cashierPath
) {
}
