package com.freshmeal.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRiderCreateRequest(
        @NotBlank(message = "登录账号不能为空")
        @Size(max = 50, message = "登录账号不能超过50个字符")
        String username,
        @NotBlank(message = "初始密码不能为空")
        @Size(min = 6, max = 50, message = "初始密码长度必须为6至50位")
        String password,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过50个字符")
        String realName,
        @NotBlank(message = "联系电话不能为空")
        @Size(max = 20, message = "联系电话不能超过20个字符")
        String phone,
        @NotNull(message = "负载上限不能为空")
        @Min(value = 1, message = "负载上限必须大于等于1")
        @Max(value = 99, message = "负载上限不能超过99")
        Integer maxOrderNum,
        Long serviceAreaId,
        @NotNull(message = "账号状态不能为空")
        @Min(value = 0, message = "账号状态只能为0或1")
        @Max(value = 1, message = "账号状态只能为0或1")
        Integer status
) {
}
