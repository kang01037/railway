package com.railway.common.model;

import lombok.Data;

/**
 * 统一返回体：{code, message, data, traceId}
 *
 * <p>静态工厂方法：
 * <ul>
 *   <li>{@link #ok()} / {@link #ok(Object)} - 成功</li>
 *   <li>{@link #fail(int, String)} / {@link #fail(ErrorCode, String)} - 失败</li>
 * </ul>
 */
@Data
public class R<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "成功", data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(com.railway.common.exception.ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(com.railway.common.exception.ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
