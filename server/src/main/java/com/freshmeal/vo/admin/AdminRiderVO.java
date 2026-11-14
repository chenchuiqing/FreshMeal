package com.freshmeal.vo.admin;

public record AdminRiderVO(
        Long id,
        String realName,
        String phone,
        Integer onlineStatus,
        Integer workStatus,
        Integer maxOrderNum,
        Integer inTransitCount,
        Long serviceAreaId,
        Boolean assignable
) {
}
