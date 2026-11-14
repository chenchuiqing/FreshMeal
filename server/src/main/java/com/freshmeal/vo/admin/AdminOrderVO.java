package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminOrderVO(
        Long id,
        String orderNo,
        Long userId,
        String username,
        String nickname,
        String phone,
        Integer status,
        String statusText,
        Integer payStatus,
        Integer payType,
        Integer deliveryType,
        BigDecimal goodsAmount,
        BigDecimal freightAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        String receiver,
        String receiverPhone,
        String receiverAddress,
        String deliverySlotName,
        LocalDate deliverySlotDate,
        LocalDateTime createTime,
        LocalDateTime payTime,
        LocalDateTime finishTime,
        LocalDateTime cancelTime,
        String cancelReason
) {
}
