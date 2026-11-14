package com.freshmeal.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MockPayNotifyRequest(
        @NotBlank(message = "支付流水号不能为空")
        @Size(max = 32)
        String payNo,

        @NotNull(message = "支付方式不能为空")
        Integer payType,

        @NotNull(message = "回调结果不能为空")
        Boolean success,

        @Size(max = 64)
        String thirdPartyNo
) {
}
