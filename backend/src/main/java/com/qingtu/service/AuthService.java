package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.Roles;
import com.qingtu.dto.LoginRequest;
import com.qingtu.dto.LoginResponse;
import com.qingtu.dto.RegisterRequest;
import com.qingtu.entity.SysUser;
import com.qingtu.mapper.SysUserMapper;
import com.qingtu.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被停用");
        }
        if (request.getRole() != null && !request.getRole().isBlank()
                && !request.getRole().equals(user.getRole())) {
            throw new BusinessException(403, "当前账号无权登录该端");
        }
        return buildResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "该账号已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.STUDENT);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return buildResponse(user);
    }

    public SysUser me(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }

    private LoginResponse buildResponse(SysUser user) {
        return new LoginResponse(jwtUtil.createToken(user), user.getId(), user.getUsername(),
                user.getName(), user.getRole());
    }
}
