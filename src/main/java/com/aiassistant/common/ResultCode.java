package com.aiassistant.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 常用业务状态码枚举。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
