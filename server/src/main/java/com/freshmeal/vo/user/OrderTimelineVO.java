package com.freshmeal.vo.user;

import java.time.LocalDateTime;

public record OrderTimelineVO(
        Integer status,
        String statusText,
        String operatorName,
        String remark,
        LocalDateTime time
) {
}
