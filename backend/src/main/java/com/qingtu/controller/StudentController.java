package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.common.ShiftStatus;
import com.qingtu.dto.LocationVO;
import com.qingtu.dto.ReservationRequest;
import com.qingtu.dto.ReservationVO;
import com.qingtu.dto.ShiftDetailVO;
import com.qingtu.dto.ShiftVO;
import com.qingtu.entity.BusRoute;
import com.qingtu.security.UserContext;
import com.qingtu.service.LocationService;
import com.qingtu.service.ReservationService;
import com.qingtu.service.RouteService;
import com.qingtu.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequireRole(Roles.STUDENT)
@RequiredArgsConstructor
public class StudentController {

    private final ShiftService shiftService;
    private final ReservationService reservationService;
    private final LocationService locationService;
    private final RouteService routeService;

    @GetMapping("/shifts")
    public Result<List<ShiftVO>> shifts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long routeId) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<ShiftVO> shifts = shiftService.list(target, routeId).stream()
                .filter(s -> !ShiftStatus.CANCELLED.equals(s.getStatus()))
                .collect(Collectors.toList());
        return Result.ok(shifts);
    }

    @GetMapping("/shifts/{id}")
    public Result<ShiftDetailVO> shiftDetail(@PathVariable Long id) {
        return Result.ok(shiftService.detail(id));
    }

    @GetMapping("/shifts/{id}/location")
    public Result<LocationVO> location(@PathVariable Long id) {
        return Result.ok(locationService.getByShift(id));
    }

    @GetMapping("/routes")
    public Result<List<BusRoute>> routes() {
        return Result.ok(routeService.list(true));
    }

    @PostMapping("/reservations")
    public Result<ReservationVO> reserve(@Valid @RequestBody ReservationRequest request) {
        return Result.ok(reservationService.create(UserContext.require().getUserId(), request.getShiftId()));
    }

    @GetMapping("/reservations")
    public Result<List<ReservationVO>> reservations() {
        return Result.ok(reservationService.listByUser(UserContext.require().getUserId()));
    }

    @PutMapping("/reservations/{id}/cancel")
    public Result<ReservationVO> cancel(@PathVariable Long id) {
        return Result.ok(reservationService.cancel(UserContext.require().getUserId(), id));
    }
}
