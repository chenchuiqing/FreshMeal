package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundVO(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String username,
        String nickname,
        BigDecimal amount,
        String reason,
        Integer status,
        Integer previousStatus,
        String adminReply,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
