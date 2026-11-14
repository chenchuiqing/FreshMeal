package com.freshmeal.vo.user;

import java.time.LocalDate;
import java.time.LocalTime;

public record DeliverySlotVO(
        Long id,
        LocalDate date,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        Integer remainingCapacity
) {
}
