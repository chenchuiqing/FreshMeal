package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @Size(max = 500, message = "商家回复不能超过500个字符")
        String reply
) {
}
