package com.freshmeal.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartItemUpdateRequest(
        Long specId,

        @Size(max = 20, message = "最多选择20个商品选项")
        List<Long> optionItemIds,

        @Min(value = 1, message = "数量至少为1")
        @Max(value = 99, message = "单次最多购买99件")
        Integer quantity,

        Boolean checked
) {
}
