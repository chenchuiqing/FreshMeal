package com.freshmeal.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderSettleRequest(
        Long addressId,

        @NotNull(message = "配送方式不能为空")
        Integer deliveryType,

        @Size(max = 100, message = "最多结算100条购物车记录")
        List<Long> cartItemIds,

        @Size(max = 20, message = "最多结算20个立即购买项")
        List<@Valid DirectPurchaseItem> directItems,

        Long userCouponId
) {
}
