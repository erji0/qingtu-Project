package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.Roles;
import com.qingtu.dto.ShiftVO;
import com.qingtu.dto.StatisticsVO;
import com.qingtu.entity.SysUser;
import com.qingtu.mapper.BusVehicleMapper;
import com.qingtu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ShiftService shiftService;
    private final SysUserMapper userMapper;
    private final BusVehicleMapper vehicleMapper;

    public StatisticsVO overview(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<ShiftVO> shifts = shiftService.list(target, null);

        StatisticsVO stats = new StatisticsVO();
        stats.setTotalShifts(shifts.size());
        int totalReservations = shifts.stream()
                .mapToInt(s -> s.getReservedCount() == null ? 0 : s.getReservedCount()).sum();
        stats.setTotalReservations(totalReservations);
        Long students = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Roles.STUDENT)
                .eq(SysUser::getStatus, 1));
        stats.setTotalStudents(students == null ? 0 : students.intValue());
        Long vehicles = vehicleMapper.selectCount(null);
        stats.setTotalVehicles(vehicles == null ? 0 : vehicles.intValue());

        int totalCapacity = shifts.stream()
                .mapToInt(s -> s.getCapacity() == null ? 0 : s.getCapacity()).sum();
        stats.setAvgOccupancyRate(totalCapacity == 0 ? 0.0
                : round1(totalReservations * 100.0 / totalCapacity));

        Map<Long, StatisticsVO.RouteStat> routeMap = new LinkedHashMap<>();
        for (ShiftVO shift : shifts) {
            StatisticsVO.RouteStat routeStat = routeMap.computeIfAbsent(shift.getRouteId(), id -> {
                StatisticsVO.RouteStat stat = new StatisticsVO.RouteStat();
                stat.setRouteId(shift.getRouteId());
                stat.setRouteName(shift.getRouteName());
                stat.setShiftCount(0);
                stat.setCapacity(0);
                stat.setReservedCount(0);
                return stat;
            });
            routeStat.setShiftCount(routeStat.getShiftCount() + 1);
            routeStat.setCapacity(routeStat.getCapacity() + (shift.getCapacity() == null ? 0 : shift.getCapacity()));
            routeStat.setReservedCount(routeStat.getReservedCount()
                    + (shift.getReservedCount() == null ? 0 : shift.getReservedCount()));
        }
        routeMap.values().forEach(stat -> stat.setOccupancyRate(stat.getCapacity() == 0 ? 0.0
                : round1(stat.getReservedCount() * 100.0 / stat.getCapacity())));
        List<StatisticsVO.RouteStat> routeStats = new ArrayList<>(routeMap.values());
        routeStats.sort(Comparator.comparing(StatisticsVO.RouteStat::getReservedCount, Comparator.reverseOrder()));
        stats.setRouteStats(routeStats);

        List<StatisticsVO.ShiftStat> shiftStats = new ArrayList<>();
        for (ShiftVO shift : shifts) {
            StatisticsVO.ShiftStat stat = new StatisticsVO.ShiftStat();
            stat.setShiftId(shift.getId());
            stat.setRouteId(shift.getRouteId());
            stat.setRouteName(shift.getRouteName());
            stat.setDepartTime(shift.getDepartTime() == null ? "" : shift.getDepartTime().toString());
            stat.setVehiclePlate(shift.getVehiclePlate());
            stat.setCapacity(shift.getCapacity());
            stat.setReservedCount(shift.getReservedCount());
            stat.setOccupancyRate(shift.getCapacity() == null || shift.getCapacity() == 0 ? 0.0
                    : round1(shift.getReservedCount() * 100.0 / shift.getCapacity()));
            stat.setStatus(shift.getStatus());
            shiftStats.add(stat);
        }
        shiftStats.sort(Comparator.comparing(StatisticsVO.ShiftStat::getOccupancyRate, Comparator.reverseOrder()));
        stats.setShiftStats(shiftStats);
        return stats;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
