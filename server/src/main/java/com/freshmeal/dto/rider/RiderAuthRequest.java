package com.freshmeal.dto.rider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiderAuthRequest(
        @NotBlank(message = "账号不能为空")
        @Size(max = 50, message = "账号长度不能超过50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6到100之间")
        String password
) {
}
