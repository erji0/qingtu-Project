package com.qingtu.controller;

import com.qingtu.common.Result;
import com.qingtu.dto.LoginRequest;
import com.qingtu.dto.LoginResponse;
import com.qingtu.dto.RegisterRequest;
import com.qingtu.entity.SysUser;
import com.qingtu.security.UserContext;
import com.qingtu.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.ok(authService.me(UserContext.require().getUserId()));
    }
}
