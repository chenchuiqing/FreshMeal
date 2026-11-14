package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminDeliveryVO(
        Long id,
        String deliveryNo,
        String orderNo,
        Integer status,
        String statusText,
        Integer assignType,
        String assignTypeText,
        Long deliveryManId,
        String deliveryManName,
        String deliveryManPhone,
        BigDecimal distance,
        BigDecimal freight,
        BigDecimal manFee,
        LocalDateTime assignTime,
        LocalDateTime pickupTime,
        LocalDateTime receiveTime,
        LocalDateTime createTime
) {
}
