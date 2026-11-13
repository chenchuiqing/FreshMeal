package com.freshmeal.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminProductVO(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String cover,
        String unit,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        Integer salesCount,
        Integer status,
        Integer isRecommend,
        Integer sort,
        Integer calories,
        List<String> healthTags,
        LocalDateTime updateTime
) {
}
