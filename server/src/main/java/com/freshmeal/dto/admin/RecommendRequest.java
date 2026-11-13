package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecommendRequest(
        @NotNull(message = "推荐标记不能为空")
        @Min(value = 0, message = "推荐标记只能为0或1")
        @Max(value = 1, message = "推荐标记只能为0或1")
        Integer isRecommend
) {
}
