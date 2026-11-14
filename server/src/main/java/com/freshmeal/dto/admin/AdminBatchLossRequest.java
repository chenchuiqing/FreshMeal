package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminBatchLossRequest(
        @NotNull(message = "报损数量不能为空")
        @Min(value = 1, message = "报损数量必须大于0")
        Integer quantity,
        @NotBlank(message = "报损原因不能为空")
        @Size(max = 255, message = "报损原因不能超过255个字符")
        String remark
) {
}
