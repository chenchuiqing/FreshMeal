package com.freshmeal.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductOptionRequest(
        Long id,
        @NotBlank(message = "选项组名称不能为空")
        @Size(max = 50, message = "选项组名称不能超过50个字符")
        String name,
        @NotNull(message = "选项组类型不能为空")
        @Min(value = 1, message = "选项组类型只能为1或2")
        @Max(value = 2, message = "选项组类型只能为1或2")
        Integer type,
        @Min(value = 0, message = "必选标记只能为0或1")
        @Max(value = 1, message = "必选标记只能为0或1")
        Integer required,
        @Min(value = 0, message = "排序值不能小于0")
        Integer sort,
        @NotEmpty(message = "选项组至少包含一个选项")
        @Valid
        List<ProductOptionItemRequest> items
) {
}
