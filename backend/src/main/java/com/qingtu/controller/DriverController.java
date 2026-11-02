package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.LocationVO;
import com.qingtu.dto.RunStatusRequest;
import com.qingtu.dto.TaskVO;
import com.qingtu.security.UserContext;
import com.qingtu.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequireRole(Roles.DRIVER)
@RequiredArgsConstructor
public class DriverController {

    private final TaskService taskService;

    @GetMapping("/tasks")
    public Result<List<TaskVO>> tasks() {
        return Result.ok(taskService.listByDriver(UserContext.require().getUserId()));
    }

    @GetMapping("/tasks/{id}")
    public Result<TaskVO> detail(@PathVariable Long id) {
        return Result.ok(taskService.detail(id, UserContext.require().getUserId()));
    }

    @PutMapping("/tasks/{id}/start")
    public Result<TaskVO> start(@PathVariable Long id) {
        return Result.ok(taskService.start(id, UserContext.require().getUserId()));
    }

    @PutMapping("/tasks/{id}/finish")
    public Result<TaskVO> finish(@PathVariable Long id) {
        return Result.ok(taskService.finish(id, UserContext.require().getUserId()));
    }

    @PutMapping("/tasks/{id}/run-status")
    public Result<LocationVO> updateRunStatus(@PathVariable Long id,
                                              @Valid @RequestBody RunStatusRequest request) {
        return Result.ok(taskService.updateRunStatus(id, UserContext.require().getUserId(),
                request.getRunStatus()));
    }

    @GetMapping("/tasks/{id}/location")
    public Result<LocationVO> location(@PathVariable Long id) {
        return Result.ok(taskService.location(id, UserContext.require().getUserId()));
    }
}
