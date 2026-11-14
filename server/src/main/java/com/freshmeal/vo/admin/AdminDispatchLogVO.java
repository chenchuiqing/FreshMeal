package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminDispatchLogVO(
        Long id,
        Long deliveryOrderId,
        String deliveryNo,
        String orderNo,
        Long candidateId,
        String candidateName,
        BigDecimal totalScore,
        BigDecimal distanceScore,
        BigDecimal loadScore,
        BigDecimal ratingScore,
        BigDecimal distanceValue,
        Integer loadValue,
        Integer isFinal,
        LocalDateTime createTime
) {
}
