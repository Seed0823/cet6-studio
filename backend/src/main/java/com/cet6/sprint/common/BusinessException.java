package com.cet6.sprint.common;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 业务校验不通过时抛出，由 {@link GlobalExceptionHandler} 统一捕获转成 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
