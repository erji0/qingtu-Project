package com.qingtu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "请输入账号")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 可选：ADMIN / STUDENT / DRIVER，用于三端登录隔离 */
    private String role;
}
