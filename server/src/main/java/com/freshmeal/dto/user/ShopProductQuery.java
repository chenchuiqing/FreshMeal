package com.freshmeal.dto.user;

import com.freshmeal.dto.common.PageQuery;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopProductQuery extends PageQuery {

    private Long categoryId;

    private String keyword;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer minCalories;

    private Integer maxCalories;

    /** 健康标签，多个标签用英文逗号分隔，取交集。 */
    private String healthTags;

    /** 排序方式：default、sales、priceAsc、priceDesc。 */
    private String sort;
}
