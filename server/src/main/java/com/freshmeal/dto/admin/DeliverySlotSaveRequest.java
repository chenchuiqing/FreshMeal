package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record DeliverySlotSaveRequest(
        @NotBlank(message = "时段名称不能为空")
        @Size(max = 30, message = "时段名称不能超过30个字符")
        String name,
        @NotNull(message = "开始时间不能为空")
        LocalTime startTime,
        @NotNull(message = "结束时间不能为空")
        LocalTime endTime,
        @NotNull(message = "下单截止时间不能为空")
        LocalTime orderDeadline,
        @NotNull(message = "每日容量不能为空")
        @Min(value = 0, message = "每日容量不能为负数")
        @Max(value = 100000, message = "每日容量不能超过100000")
        Integer dailyCapacity,
        @NotNull(message = "可预约天数不能为空")
        @Min(value = 0, message = "可预约天数不能为负数")
        @Max(value = 30, message = "可预约天数不能超过30")
        Integer advanceDays,
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能为负数")
        @Max(value = 9999, message = "排序值不能超过9999")
        Integer sort,
        @NotNull(message = "时段状态不能为空")
        @Min(value = 0, message = "时段状态只能为0或1")
        @Max(value = 1, message = "时段状态只能为0或1")
        Integer status
) {
}
