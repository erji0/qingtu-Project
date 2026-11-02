package com.qingtu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "请输入账号")
    @Size(min = 4, max = 20, message = "账号长度需为 4-20 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 30, message = "密码长度需为 6-30 个字符")
    private String password;

    @NotBlank(message = "请输入姓名")
    private String name;

    private String phone;
}
