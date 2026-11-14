package com.freshmeal.service;

public record OrderOperator(
        int operatorType,
        Long operatorId,
        String operatorName,
        String remark,
        Integer payType
) {

    public static OrderOperator system(String remark) {
        return new OrderOperator(0, null, "系统", remark, null);
    }

    public static OrderOperator user(Long userId, String remark) {
        return new OrderOperator(1, userId, "用户", remark, null);
    }

    public static OrderOperator rider(Long riderId, String remark) {
        return new OrderOperator(3, riderId, "配送员", remark, null);
    }
}
