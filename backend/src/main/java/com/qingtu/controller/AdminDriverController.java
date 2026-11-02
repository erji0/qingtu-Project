package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.RegisterRequest;
import com.qingtu.entity.SysUser;
import com.qingtu.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminDriverController {

    private final DriverService driverService;

    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.ok(driverService.list());
    }

    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(driverService.create(request));
    }

    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody RegisterRequest request) {
        return Result.ok(driverService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        driverService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<SysUser> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(driverService.updateStatus(id, status));
    }
}
