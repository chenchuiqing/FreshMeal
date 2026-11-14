package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryTraceVO(
        Integer status,
        String statusText,
        String address,
        String remark,
        BigDecimal longitude,
        BigDecimal latitude,
        LocalDateTime time
) {
}
