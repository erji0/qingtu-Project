package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.LocationVO;
import com.qingtu.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/monitor")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminMonitorController {

    private final LocationService locationService;

    @GetMapping("/locations")
    public Result<List<LocationVO>> locations() {
        return Result.ok(locationService.listActive());
    }
}
