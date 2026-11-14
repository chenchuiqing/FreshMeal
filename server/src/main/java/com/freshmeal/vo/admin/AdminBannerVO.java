package com.freshmeal.vo.admin;

import java.time.LocalDateTime;

public record AdminBannerVO(
        Long id,
        String title,
        String image,
        String link,
        Integer sort,
        Integer status,
        LocalDateTime createTime
) {
}
