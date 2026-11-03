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
import com.qingtu.entity.BusReservation;
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

    /**
     * 获取班次列表接口
     * 根据日期和路线ID筛选班次信息，排除已取消的班次
     *
     * @param date 查询日期，可选参数，格式为ISO日期格式(yyyy-MM-dd)，未提供则默认为当前日期
     * @param routeId 路线ID，可选参数，用于筛选特定路线的班次
     * @return 返回Result对象，包含班次列表数据(List<ShiftVO>)
     */
    @GetMapping("/shifts")
    public Result<List<ShiftVO>> shifts(
        // 日期参数，可选，使用ISO日期格式
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        // 路线ID参数，可选
            @RequestParam(required = false) Long routeId) {
        // 如果未提供日期参数，则使用当前日期
        LocalDate target = date == null ? LocalDate.now() : date;
        // 获取班次列表，过滤掉已取消的班次，并转换为List
        List<ShiftVO> shifts = shiftService.list(target, routeId).stream()
            // 过滤条件：班次状态不等于已取消
                .filter(s -> !ShiftStatus.CANCELLED.equals(s.getStatus()))
                .collect(Collectors.toList());
        return Result.ok(shifts);
    }

    @GetMapping("/shifts/{id}")
    public Result<ShiftDetailVO> shiftDetail(@PathVariable Long id) {
        ShiftDetailVO detail = shiftService.detail(id);
        BusReservation mine = reservationService.findActiveReservation(UserContext.require().getUserId(), id);
        if (mine != null) {
            detail.setMyReservationId(mine.getId());
            detail.setMyReservationStatus(mine.getStatus());
        }
        return Result.ok(detail);
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

    @PutMapping("/reservations/{id}/sign")
    public Result<ReservationVO> sign(@PathVariable Long id) {
        return Result.ok(reservationService.sign(UserContext.require().getUserId(), id));
    }
}
