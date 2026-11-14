package com.freshmeal.dto.admin;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
        @NotNull(message = "用户不能为空")
        Long userId
) {
}
