package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminBatchSaveRequest(
        @NotNull(message = "商品不能为空")
        Long productId,
        @NotBlank(message = "批次号不能为空")
        @Size(max = 32, message = "批次号不能超过32个字符")
        String batchNo,
        @NotNull(message = "生产日期不能为空")
        LocalDate productionDate,
        @NotNull(message = "到期日期不能为空")
        LocalDate expireDate,
        @NotNull(message = "批次库存不能为空")
        @Min(value = 0, message = "批次库存不能为负数")
        Integer stock,
        @NotNull(message = "锁定库存不能为空")
        @Min(value = 0, message = "锁定库存不能为负数")
        Integer lockStock,
        @Size(max = 255, message = "备注不能超过255个字符")
        String remark
) {
}
