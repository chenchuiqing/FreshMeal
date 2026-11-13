package com.freshmeal.vo.admin;

import com.freshmeal.vo.product.ProductImageVO;
import com.freshmeal.vo.product.ProductOptionVO;
import com.freshmeal.vo.product.ProductSpecVO;
import java.math.BigDecimal;
import java.util.List;

public record AdminProductDetailVO(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String cover,
        String detail,
        Integer productType,
        BigDecimal price,
        BigDecimal originalPrice,
        String unit,
        Integer stock,
        Integer salesCount,
        Integer status,
        Integer isRecommend,
        Integer sort,
        Integer calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal carbohydrate,
        BigDecimal sodium,
        String nutritionDesc,
        List<String> allergens,
        Integer storageType,
        Integer shelfLifeDays,
        String producer,
        String heatingMethod,
        List<String> healthTags,
        List<ProductSpecVO> specs,
        List<ProductOptionVO> options,
        List<ProductImageVO> images
) {
}
