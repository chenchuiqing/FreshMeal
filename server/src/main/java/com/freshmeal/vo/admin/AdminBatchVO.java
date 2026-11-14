package com.freshmeal.vo.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminBatchVO(
        Long id,
        Long productId,
        String productName,
        String batchNo,
        LocalDate productionDate,
        LocalDate expireDate,
        Integer stock,
        Integer lockStock,
        Integer status,
        String statusText,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
