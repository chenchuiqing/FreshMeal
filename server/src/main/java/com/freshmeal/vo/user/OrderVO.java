package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderVO(
        Long id,
        String orderNo,
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
        String remark,
        String cancelReason,
        LocalDateTime createTime,
        LocalDateTime payTime,
        LocalDateTime cancelTime,
        PaymentVO payment,
        List<OrderItemVO> items,
        List<OrderTimelineVO> timeline
) {
}
