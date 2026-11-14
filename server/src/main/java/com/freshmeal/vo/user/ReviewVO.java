package com.freshmeal.vo.user;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewVO(
        Long id,
        Long userId,
        Long productId,
        String productName,
        Integer rating,
        String content,
        List<String> images,
        String reply,
        LocalDateTime createTime
) {
}
