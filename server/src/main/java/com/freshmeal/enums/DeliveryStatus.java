package com.freshmeal.enums;

import java.util.Arrays;

public enum DeliveryStatus {
    WAIT_ASSIGN(0, "待分配"),
    ASSIGNED(1, "已分配"),
    PICKING_UP(2, "取货中"),
    DELIVERING(3, "配送中"),
    DELIVERED(4, "已送达");

    private final int code;
    private final String description;

    DeliveryStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeliveryStatus of(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("配送状态不能为空");
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知配送状态: " + code));
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
