package com.freshmeal.enums;

public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(40000, "参数校验失败"),
    PRODUCT_UNAVAILABLE(40001, "商品已下架"),
    UPLOAD_TYPE_UNSUPPORTED(40002, "仅支持 JPG、PNG、WebP 格式的图片"),
    UPLOAD_EMPTY(40003, "上传文件不能为空"),
    UPLOAD_SIZE_EXCEEDED(40004, "图片大小不能超过10MB"),
    CART_ITEM_UNAVAILABLE(40005, "购物车商品不可购买"),
    ADDRESS_NOT_FOUND(40403, "收货地址不存在"),
    DELIVERY_AREA_NOT_FOUND(40404, "当前地址暂不支持配送"),
    INVENTORY_INSUFFICIENT(40903, "商品库存不足"),
    ORDER_NOT_FOUND(40405, "订单不存在"),
    PAYMENT_NOT_FOUND(40406, "支付流水不存在"),
    ORDER_STATE_INVALID(40904, "订单当前状态不允许该操作"),
    PAYMENT_EXPIRED(40905, "支付已超时"),
    INVALID_OPTION(40006, "商品选项不合法"),
    UNAUTHORIZED(40101, "未认证或登录状态已失效"),
    FORBIDDEN(40301, "无权访问该资源"),
    NOT_FOUND(40400, "请求资源不存在"),
    PRODUCT_NOT_FOUND(40401, "商品不存在"),
    CATEGORY_NOT_FOUND(40402, "分类不存在"),
    CONFLICT(40900, "当前状态不允许该操作"),
    CATEGORY_HAS_CHILDREN(40901, "该分类下存在子分类，无法删除"),
    CATEGORY_HAS_PRODUCTS(40902, "该分类下存在商品，无法删除"),
    INTERNAL_ERROR(50000, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
