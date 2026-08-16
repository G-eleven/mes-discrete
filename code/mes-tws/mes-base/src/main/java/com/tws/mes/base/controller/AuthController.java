package com.tws.mes.base.controller;

import com.tws.mes.base.service.AuthService;
import com.tws.mes.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 登录相关接口（/api/auth/login 放行，其余需要登录） */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    public static class LoginDTO {
        private String username;
        private String password;
    }

    @PostMapping("/login")
    public Result<AuthService.LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto.getUsername(), dto.getPassword()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<AuthService.LoginVO> me() {
        return Result.ok(authService.me());
    }
}
