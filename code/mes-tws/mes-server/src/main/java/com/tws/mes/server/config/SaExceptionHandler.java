package com.tws.mes.server.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.tws.mes.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常 → 统一 Result 结构。
 * 前端约定：401 跳登录页，403 提示无权限。
 */
@RestControllerAdvice
public class SaExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(401, "未登录或登录已过期");
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNoRole(NotRoleException e) {
        return Result.fail(403, "无权限执行该操作，需要角色: " + e.getRole());
    }
}
