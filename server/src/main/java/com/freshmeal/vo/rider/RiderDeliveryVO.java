package com.freshmeal.vo.rider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiderDeliveryVO(
        Long id,
        String deliveryNo,
        String orderNo,
        Integer status,
        String statusText,
        Integer assignType,
        String receiver,
        String receiverPhone,
        String receiverAddress,
        BigDecimal distance,
        BigDecimal freight,
        BigDecimal manFee,
        LocalDateTime slotStart,
        LocalDateTime createTime,
        LocalDateTime assignTime,
        LocalDateTime pickupTime,
        LocalDateTime receiveTime
) {
}
