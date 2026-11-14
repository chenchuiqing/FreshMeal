package com.freshmeal.vo.admin;

import java.time.LocalDateTime;

public record AdminInventoryLogVO(
        Long id,
        Long productId,
        String productName,
        Long batchId,
        String batchNo,
        Integer changeType,
        String changeTypeText,
        Integer changeNum,
        Integer beforeStock,
        Integer afterStock,
        String refType,
        String refNo,
        Long operatorId,
        String remark,
        LocalDateTime createTime
) {
}
