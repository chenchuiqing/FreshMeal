package com.freshmeal.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 立即购买项：不经过购物车直接下单的商品配置。
 * 金额由服务端按当前规格与选项重新计算，请求中不含任何价格字段。
 */
public record DirectPurchaseItem(
        @NotNull(message = "商品ID不能为空")
        Long productId,

        Long specId,

        @Size(max = 20, message = "最多选择20个商品选项")
        List<Long> optionItemIds,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        @Max(value = 99, message = "单次最多购买99件")
        Integer quantity
) {
}
