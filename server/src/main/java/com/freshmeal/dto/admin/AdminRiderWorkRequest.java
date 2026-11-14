package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminRiderWorkRequest(
        @NotNull(message = "在线状态不能为空")
        @Min(value = 0, message = "在线状态只能为0或1")
        @Max(value = 1, message = "在线状态只能为0或1")
        Integer onlineStatus,
        @NotNull(message = "工作状态不能为空")
        @Min(value = 0, message = "工作状态只能为0、1或2")
        @Max(value = 2, message = "工作状态只能为0、1或2")
        Integer workStatus
) {
}
