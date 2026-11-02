package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.entity.BusRoute;
import com.qingtu.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/routes")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminRouteController {

    private final RouteService routeService;

    @GetMapping
    public Result<List<BusRoute>> list() {
        return Result.ok(routeService.list(false));
    }

    @PostMapping
    public Result<BusRoute> create(@RequestBody BusRoute route) {
        return Result.ok(routeService.create(route));
    }

    @PutMapping("/{id}")
    public Result<BusRoute> update(@PathVariable Long id, @RequestBody BusRoute route) {
        return Result.ok(routeService.update(id, route));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return Result.ok();
    }
}
