package com.aiassistant.common;

import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由 {@link GlobalExceptionHandler} 转为统一响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，对应 {@link ResultCode} 或自定义码。 */
    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
