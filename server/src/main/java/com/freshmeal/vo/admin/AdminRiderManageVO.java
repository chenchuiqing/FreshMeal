package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRiderManageVO(
        Long id,
        String username,
        String realName,
        String phone,
        Long serviceAreaId,
        String serviceAreaName,
        Integer onlineStatus,
        Integer workStatus,
        Integer maxOrderNum,
        Integer inTransitCount,
        Integer todayOrderCount,
        Integer totalOrderCount,
        BigDecimal totalIncome,
        BigDecimal rating,
        Integer status,
        Boolean assignable,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
