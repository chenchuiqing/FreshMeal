package com.freshmeal.dto.admin;

import jakarta.validation.constraints.NotNull;

public record DeliveryAssignRequest(
        @NotNull(message = "配送员不能为空")
        Long riderId
) {
}
