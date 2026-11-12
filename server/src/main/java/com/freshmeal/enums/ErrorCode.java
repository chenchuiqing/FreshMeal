package com.freshmeal.enums;

public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(40000, "参数校验失败"),
    UNAUTHORIZED(40101, "未认证或登录状态已失效"),
    FORBIDDEN(40301, "无权访问该资源"),
    NOT_FOUND(40400, "请求资源不存在"),
    CONFLICT(40900, "当前状态不允许该操作"),
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
