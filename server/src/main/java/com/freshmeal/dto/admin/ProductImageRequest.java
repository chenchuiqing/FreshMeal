package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        Long id,
        @NotBlank(message = "图片地址不能为空")
        @Size(max = 255, message = "图片地址不能超过255个字符")
        String imageUrl,
        @Min(value = 0, message = "排序值不能小于0")
        Integer sort,
        @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1")
        Integer status
) {
}
