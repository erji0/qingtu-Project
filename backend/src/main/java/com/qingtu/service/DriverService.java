package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.Roles;
import com.qingtu.dto.RegisterRequest;
import com.qingtu.entity.DriverTask;
import com.qingtu.entity.SysUser;
import com.qingtu.mapper.DriverTaskMapper;
import com.qingtu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final SysUserMapper userMapper;
    private final DriverTaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;

    public List<SysUser> list() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Roles.DRIVER)
                .orderByAsc(SysUser::getId));
    }

    public SysUser create(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "该账号已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.DRIVER);
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public SysUser update(Long id, RegisterRequest request) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null || !Roles.DRIVER.equals(exists.getRole())) {
            throw new BusinessException("司机不存在");
        }
        Long conflict = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .ne(SysUser::getId, id));
        if (conflict != null && conflict > 0) {
            throw new BusinessException(400, "该账号已被使用");
        }
        exists.setUsername(request.getUsername());
        exists.setName(request.getName());
        exists.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            exists.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(exists);
        return exists;
    }

    @Transactional
    public void delete(Long id) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null || !Roles.DRIVER.equals(exists.getRole())) {
            throw new BusinessException("司机不存在");
        }
        Long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<DriverTask>()
                .eq(DriverTask::getDriverId, id));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException("该司机已有分配任务，不能删除");
        }
        userMapper.deleteById(id);
    }

    public SysUser updateStatus(Long id, Integer status) {
        SysUser exists = userMapper.selectById(id);
        if (exists == null || !Roles.DRIVER.equals(exists.getRole())) {
            throw new BusinessException("司机不存在");
        }
        exists.setStatus(status);
        userMapper.updateById(exists);
        return exists;
    }
}
