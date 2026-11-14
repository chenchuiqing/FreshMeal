package com.freshmeal.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AddressSaveRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人不能超过50个字符")
        String receiver,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
        String phone,

        @NotBlank(message = "省份不能为空")
        @Size(max = 30, message = "省份不能超过30个字符")
        String province,

        @NotBlank(message = "城市不能为空")
        @Size(max = 30, message = "城市不能超过30个字符")
        String city,

        @NotBlank(message = "区县不能为空")
        @Size(max = 30, message = "区县不能超过30个字符")
        String district,

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 200, message = "详细地址不能超过200个字符")
        String detail,

        @NotNull(message = "请提供经度")
        BigDecimal longitude,

        @NotNull(message = "请提供纬度")
        BigDecimal latitude,

        @Size(max = 20, message = "地址标签不能超过20个字符")
        String tag,

        @NotNull(message = "默认地址标识不能为空")
        Boolean isDefault
) {
}
