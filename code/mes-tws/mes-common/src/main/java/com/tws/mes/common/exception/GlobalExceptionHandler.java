package com.tws.mes.common.exception;

import com.tws.mes.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常统一转成 Result 结构。
 * 注意：Sa-Token 的未登录/无权限异常在 mes-server 的 SaExceptionHandler 处理
 * （依赖了 sa-token 的模块才能引用这些异常类）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常: " + e.getMessage());
    }
}
