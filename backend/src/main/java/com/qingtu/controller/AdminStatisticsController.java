package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.StatisticsVO;
import com.qingtu.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/statistics")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public Result<StatisticsVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(statisticsService.overview(date));
    }
}
