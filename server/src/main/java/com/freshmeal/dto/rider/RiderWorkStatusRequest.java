package com.freshmeal.dto.rider;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RiderWorkStatusRequest(
        @NotNull(message = "在线状态不能为空")
        @Min(value = 0, message = "在线状态不合法")
        @Max(value = 1, message = "在线状态不合法")
        Integer onlineStatus,

        @NotNull(message = "工作状态不能为空")
        @Min(value = 0, message = "工作状态不合法")
        @Max(value = 2, message = "工作状态不合法")
        Integer workStatus
) {
}
