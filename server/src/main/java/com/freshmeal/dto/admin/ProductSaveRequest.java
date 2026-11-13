package com.freshmeal.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductSaveRequest(
        @NotNull(message = "商品分类不能为空")
        Long categoryId,
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过100个字符")
        String name,
        @Size(max = 200, message = "副标题不能超过200个字符")
        String subtitle,
        @NotBlank(message = "商品主图不能为空")
        @Size(max = 255, message = "商品主图地址不能超过255个字符")
        String cover,
        String detail,
        @Min(value = 1, message = "商品类型只能为1或2")
        @Max(value = 2, message = "商品类型只能为1或2")
        Integer productType,
        @NotNull(message = "商品售价不能为空")
        @DecimalMin(value = "0.01", message = "商品售价必须大于0")
        @Digits(integer = 8, fraction = 2, message = "商品售价最多保留两位小数")
        BigDecimal price,
        @DecimalMin(value = "0.00", message = "划线原价不能小于0")
        @Digits(integer = 8, fraction = 2, message = "划线原价最多保留两位小数")
        BigDecimal originalPrice,
        @NotBlank(message = "商品单位不能为空")
        @Size(max = 20, message = "商品单位不能超过20个字符")
        String unit,
        @NotNull(message = "商品库存不能为空")
        @Min(value = 0, message = "商品库存不能小于0")
        Integer stock,
        @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1")
        Integer status,
        @Min(value = 0, message = "推荐标记只能为0或1")
        @Max(value = 1, message = "推荐标记只能为0或1")
        Integer isRecommend,
        @Min(value = 0, message = "排序值不能小于0")
        Integer sort,
        @NotNull(message = "热量不能为空")
        @Min(value = 0, message = "热量不能小于0")
        Integer calories,
        @NotNull(message = "蛋白质不能为空")
        @DecimalMin(value = "0.0", message = "蛋白质不能小于0")
        BigDecimal protein,
        @NotNull(message = "脂肪不能为空")
        @DecimalMin(value = "0.0", message = "脂肪不能小于0")
        BigDecimal fat,
        @NotNull(message = "碳水化合物不能为空")
        @DecimalMin(value = "0.0", message = "碳水化合物不能小于0")
        BigDecimal carbohydrate,
        @NotNull(message = "钠含量不能为空")
        @DecimalMin(value = "0.0", message = "钠含量不能小于0")
        BigDecimal sodium,
        @Size(max = 255, message = "营养说明不能超过255个字符")
        String nutritionDesc,
        List<@Size(max = 20, message = "过敏原名称不能超过20个字符") String> allergens,
        @NotNull(message = "储存方式不能为空")
        @Min(value = 1, message = "储存方式只能为1、2或3")
        @Max(value = 3, message = "储存方式只能为1、2或3")
        Integer storageType,
        @NotNull(message = "保质期不能为空")
        @Min(value = 0, message = "保质期不能小于0")
        @Max(value = 3650, message = "保质期不能超过3650天")
        Integer shelfLifeDays,
        @Size(max = 100, message = "生产商不能超过100个字符")
        String producer,
        @Size(max = 100, message = "食用方式不能超过100个字符")
        String heatingMethod,
        List<@Size(max = 20, message = "健康标签不能超过20个字符") String> healthTags,
        @NotEmpty(message = "商品至少包含一个规格")
        @Valid
        List<ProductSpecRequest> specs,
        @Valid
        List<ProductOptionRequest> options,
        @Valid
        List<ProductImageRequest> images
) {
}
