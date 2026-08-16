package com.tws.mes.common.exception;

import lombok.Getter;

/** 业务异常：可预期的、面向用户提示的错误 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
