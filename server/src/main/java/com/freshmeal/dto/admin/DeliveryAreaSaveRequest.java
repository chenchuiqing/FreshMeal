package com.freshmeal.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DeliveryAreaSaveRequest(
        @NotBlank(message = "区域名称不能为空")
        @Size(max = 50, message = "区域名称不能超过50个字符")
        String name,
        @NotBlank(message = "所属城市不能为空")
        @Size(max = 30, message = "所属城市不能超过30个字符")
        String city,
        @Size(max = 30, message = "覆盖区县不能超过30个字符")
        String district,
        @NotNull(message = "起步配送费不能为空")
        @DecimalMin(value = "0", message = "起步配送费不能为负数")
        BigDecimal baseFee,
        @NotNull(message = "每公里加价不能为空")
        @DecimalMin(value = "0", message = "每公里加价不能为负数")
        BigDecimal perKmFee,
        @NotNull(message = "免配送费门槛不能为空")
        @DecimalMin(value = "0", message = "免配送费门槛不能为负数")
        BigDecimal freeThreshold,
        @NotNull(message = "夜间附加费不能为空")
        @DecimalMin(value = "0", message = "夜间附加费不能为负数")
        BigDecimal nightSurcharge,
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能为负数")
        @Max(value = 9999, message = "排序值不能超过9999")
        Integer sort,
        @NotNull(message = "区域状态不能为空")
        @Min(value = 0, message = "区域状态只能为0或1")
        @Max(value = 1, message = "区域状态只能为0或1")
        Integer status
) {
}
