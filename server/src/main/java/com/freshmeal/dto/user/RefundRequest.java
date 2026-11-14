package com.freshmeal.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @NotBlank(message = "退款原因不能为空")
        @Size(max = 500, message = "退款原因不能超过500个字符")
        String reason
) {
}
