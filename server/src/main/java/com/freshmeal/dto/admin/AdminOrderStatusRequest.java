package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusRequest(
        @NotNull(message = "目标状态不能为空")
        @Min(value = 0, message = "订单状态不合法")
        @Max(value = 9, message = "订单状态不合法")
        Integer targetStatus,
        String reason
) {
}
