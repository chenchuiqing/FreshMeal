package com.freshmeal.vo.user;

import java.math.BigDecimal;
import java.util.List;

public record ShopProductVO(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String cover,
        String unit,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal carbohydrate,
        BigDecimal sodium,
        List<String> healthTags,
        Integer stock,
        Integer salesCount,
        Integer status,
        boolean available
) {
}
