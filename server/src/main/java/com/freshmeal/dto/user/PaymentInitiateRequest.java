package com.freshmeal.dto.user;

import jakarta.validation.constraints.NotNull;

public record PaymentInitiateRequest(
        @NotNull(message = "支付方式不能为空")
        Integer payType
) {
}
