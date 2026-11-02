package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.AssignRequest;
import com.qingtu.dto.ShiftDetailVO;
import com.qingtu.dto.ShiftVO;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.SysUser;
import com.qingtu.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/shifts")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminShiftController {

    private final ShiftService shiftService;

    @GetMapping
    public Result<List<ShiftVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long routeId) {
        return Result.ok(shiftService.list(date, routeId));
    }

    @GetMapping("/drivers")
    public Result<List<SysUser>> drivers() {
        return Result.ok(shiftService.listDrivers());
    }

    @GetMapping("/{id}")
    public Result<ShiftDetailVO> detail(@PathVariable Long id) {
        return Result.ok(shiftService.detail(id));
    }

    @PostMapping
    public Result<ShiftDetailVO> create(@RequestBody BusShift shift) {
        return Result.ok(shiftService.create(shift));
    }

    @PutMapping("/{id}")
    public Result<ShiftDetailVO> update(@PathVariable Long id, @RequestBody BusShift shift) {
        return Result.ok(shiftService.update(id, shift));
    }

    @PutMapping("/{id}/assign")
    public Result<ShiftDetailVO> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return Result.ok(shiftService.assign(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shiftService.delete(id);
        return Result.ok();
    }
}
