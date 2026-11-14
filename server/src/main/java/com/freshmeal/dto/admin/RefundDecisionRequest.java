package com.freshmeal.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundDecisionRequest(
        @NotNull(message = "处理结果不能为空")
        Integer status,

        @Size(max = 500, message = "处理说明不能超过500个字符")
        String adminReply
) {
}
