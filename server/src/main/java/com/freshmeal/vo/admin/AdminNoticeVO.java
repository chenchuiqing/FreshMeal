package com.freshmeal.vo.admin;

import java.time.LocalDateTime;

public record AdminNoticeVO(
        Long id,
        String title,
        String content,
        Integer type,
        Integer status,
        LocalDateTime createTime
) {
}
