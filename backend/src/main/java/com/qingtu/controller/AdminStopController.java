package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.entity.BusStop;
import com.qingtu.service.StopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stops")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminStopController {

    private final StopService stopService;

    @GetMapping
    public Result<List<BusStop>> list(@RequestParam(required = false) Long routeId) {
        return Result.ok(stopService.list(routeId));
    }

    @PostMapping
    public Result<BusStop> create(@RequestBody BusStop stop) {
        return Result.ok(stopService.create(stop));
    }

    @PutMapping("/{id}")
    public Result<BusStop> update(@PathVariable Long id, @RequestBody BusStop stop) {
        return Result.ok(stopService.update(id, stop));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stopService.delete(id);
        return Result.ok();
    }
}
