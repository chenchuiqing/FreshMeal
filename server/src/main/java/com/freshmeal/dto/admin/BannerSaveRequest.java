package com.freshmeal.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BannerSaveRequest(
        @NotBlank(message = "轮播标题不能为空")
        @Size(max = 100, message = "轮播标题不能超过100个字符")
        String title,

        @NotBlank(message = "轮播图片不能为空")
        @Size(max = 255, message = "轮播图片地址过长")
        String image,

        @Size(max = 255, message = "轮播链接过长")
        String link,

        Integer sort,

        Integer status
) {
}
