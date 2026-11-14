package com.freshmeal.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReviewSubmitRequest(
        @NotNull(message = "商品不能为空")
        Long productId,

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分必须在1到5之间")
        @Max(value = 5, message = "评分必须在1到5之间")
        Integer rating,

        @Size(max = 500, message = "评价内容不能超过500个字符")
        String content,

        @Size(max = 9, message = "最多上传9张图片")
        List<@Size(max = 500, message = "图片地址过长") String> images
) {
}
