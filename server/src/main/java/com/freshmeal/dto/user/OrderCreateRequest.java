package com.freshmeal.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "收货地址不能为空")
        Long addressId,

        @NotNull(message = "配送方式不能为空")
        Integer deliveryType,

        @Size(max = 100, message = "请选择1到100条购物车记录")
        List<Long> cartItemIds,

        @Size(max = 20, message = "最多结算20个立即购买项")
        List<@Valid DirectPurchaseItem> directItems,

        Long deliverySlotId,

        LocalDate deliverySlotDate,

        @Size(max = 200, message = "订单备注不能超过200个字符")
        String remark,

        Long userCouponId
) {
}
