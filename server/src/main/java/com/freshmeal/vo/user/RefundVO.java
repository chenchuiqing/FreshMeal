package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundVO(
        Long id,
        String orderNo,
        BigDecimal amount,
        String reason,
        Integer status,
        Integer previousStatus,
        String adminReply,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
