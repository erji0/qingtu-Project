package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.entity.BusVehicle;
import com.qingtu.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicles")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminVehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public Result<List<BusVehicle>> list(@RequestParam(required = false) String status) {
        return Result.ok(vehicleService.list(status));
    }

    @PostMapping
    public Result<BusVehicle> create(@RequestBody BusVehicle vehicle) {
        return Result.ok(vehicleService.create(vehicle));
    }

    @PutMapping("/{id}")
    public Result<BusVehicle> update(@PathVariable Long id, @RequestBody BusVehicle vehicle) {
        return Result.ok(vehicleService.update(id, vehicle));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return Result.ok();
    }
}
