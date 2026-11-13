package com.freshmeal.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductSpecRequest(
        Long id,
        @NotBlank(message = "规格名称不能为空")
        @Size(max = 50, message = "规格名称不能超过50个字符")
        String name,
        @NotNull(message = "规格加价不能为空")
        @DecimalMin(value = "0.00", message = "规格加价不能小于0")
        BigDecimal priceDelta,
        @NotNull(message = "规格库存不能为空")
        @Min(value = 0, message = "规格库存不能小于0")
        Integer stock,
        @NotNull(message = "规格热量不能为空")
        @Min(value = 0, message = "规格热量不能小于0")
        Integer calories,
        @Min(value = 0, message = "默认标记只能为0或1")
        @Max(value = 1, message = "默认标记只能为0或1")
        Integer isDefault,
        @Min(value = 0, message = "排序值不能小于0")
        Integer sort,
        @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1")
        Integer status
) {
}
