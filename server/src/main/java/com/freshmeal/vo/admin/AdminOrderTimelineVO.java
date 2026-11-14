package com.freshmeal.vo.admin;

import java.time.LocalDateTime;

public record AdminOrderTimelineVO(
        Integer fromStatus,
        Integer toStatus,
        String toStatusText,
        String operatorName,
        String remark,
        LocalDateTime createTime
) {
}
