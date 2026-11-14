package com.freshmeal.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeSaveRequest(
        @NotBlank(message = "公告标题不能为空")
        @Size(max = 200, message = "公告标题不能超过200个字符")
        String title,

        @Size(max = 5000, message = "公告内容不能超过5000个字符")
        String content,

        Integer type,

        Integer status
) {
}
