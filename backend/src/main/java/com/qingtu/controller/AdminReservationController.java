package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.ReservationVO;
import com.qingtu.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public Result<List<ReservationVO>> list(
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.listAll(shiftId, date));
    }
}
