package com.freshmeal.vo.user;

import java.time.LocalDateTime;

public record NoticeVO(
        Long id,
        String title,
        String content,
        Integer type,
        LocalDateTime createTime
) {
}
