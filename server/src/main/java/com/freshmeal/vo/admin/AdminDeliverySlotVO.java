package com.freshmeal.vo.admin;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminDeliverySlotVO(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime orderDeadline,
        Integer dailyCapacity,
        Integer advanceDays,
        Integer sort,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
