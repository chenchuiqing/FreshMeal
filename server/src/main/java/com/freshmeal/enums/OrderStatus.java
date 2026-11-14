package com.freshmeal.enums;

import java.util.Arrays;
import java.util.Set;

public enum OrderStatus {

    WAIT_PAY(0, "待支付", Set.of(1, 9)),
    WAIT_ACCEPT(1, "待接单", Set.of(2, 7, 9)),
    PREPARING(2, "备餐中", Set.of(3, 4, 7)),
    WAIT_SLOT(3, "待配送时段", Set.of(4, 7)),
    WAIT_DELIVER(4, "待配送", Set.of(5, 7, 9)),
    DELIVERING(5, "配送中", Set.of(6, 7)),
    FINISHED(6, "已完成", Set.of(7)),
    REFUNDING(7, "退款中", Set.of(8, 1, 2, 3, 4, 5, 6)),
    REFUNDED(8, "已退款", Set.of()),
    CANCELLED(9, "已取消", Set.of());

    private final int code;
    private final String description;
    private final Set<Integer> nextCodes;

    OrderStatus(int code, String description, Set<Integer> nextCodes) {
        this.code = code;
        this.description = description;
        this.nextCodes = nextCodes;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("订单状态不能为空");
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知订单状态: " + code));
    }

    public boolean canTransferTo(OrderStatus target) {
        return nextCodes.contains(target.code);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
