package com.freshmeal.vo.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReviewVO(
        Long id,
        Long userId,
        String username,
        String nickname,
        Long orderId,
        String orderNo,
        Long productId,
        String productName,
        Integer rating,
        String content,
        List<String> images,
        String reply,
        LocalDateTime createTime
) {
}
