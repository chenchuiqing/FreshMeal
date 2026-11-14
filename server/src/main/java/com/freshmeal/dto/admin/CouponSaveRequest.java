package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponSaveRequest(
        @NotBlank(message = "优惠券名称不能为空")
        @Size(max = 50, message = "优惠券名称不能超过50个字符")
        String name,

        @NotNull(message = "优惠券类型不能为空")
        @Min(value = 1, message = "优惠券类型不合法")
        @Max(value = 2, message = "优惠券类型不合法")
        Integer type,

        @NotNull(message = "使用门槛不能为空")
        @Min(value = 0, message = "使用门槛不能为负数")
        BigDecimal threshold,

        @NotNull(message = "优惠金额不能为空")
        @Min(value = 0, message = "优惠金额不能为负数")
        BigDecimal amount,

        @NotNull(message = "生效时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "失效时间不能为空")
        LocalDateTime endTime,

        @NotNull(message = "发放总量不能为空")
        @Min(value = 1, message = "发放总量至少为1")
        Integer totalCount
) {
}
