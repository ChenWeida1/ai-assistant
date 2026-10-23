package com.aiassistant.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应外壳：{@code code} / {@code message} / {@code data}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码，成功一般为 200。 */
    private int code;
    /** 提示文案。 */
    private String message;
    /** 成功时的载荷；失败时通常为 null。 */
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(String message) {
        return fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
