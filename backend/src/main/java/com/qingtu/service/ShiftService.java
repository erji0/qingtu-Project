package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.ReservationStatus;
import com.qingtu.common.Roles;
import com.qingtu.common.ShiftStatus;
import com.qingtu.common.TaskStatus;
import com.qingtu.common.VehicleStatus;
import com.qingtu.dto.AssignRequest;
import com.qingtu.dto.ShiftDetailVO;
import com.qingtu.dto.ShiftVO;
import com.qingtu.entity.BusReservation;
import com.qingtu.entity.BusRoute;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusStop;
import com.qingtu.entity.BusVehicle;
import com.qingtu.entity.BusVehicleLocation;
import com.qingtu.entity.DriverTask;
import com.qingtu.entity.SysUser;
import com.qingtu.mapper.BusReservationMapper;
import com.qingtu.mapper.BusRouteMapper;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusStopMapper;
import com.qingtu.mapper.BusVehicleLocationMapper;
import com.qingtu.mapper.BusVehicleMapper;
import com.qingtu.mapper.DriverTaskMapper;
import com.qingtu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final BusShiftMapper shiftMapper;
    private final BusRouteMapper routeMapper;
    private final BusVehicleMapper vehicleMapper;
    private final SysUserMapper userMapper;
    private final BusStopMapper stopMapper;
    private final BusReservationMapper reservationMapper;
    private final DriverTaskMapper taskMapper;
    private final BusVehicleLocationMapper locationMapper;

    public List<ShiftVO> list(LocalDate date, Long routeId) {
        LambdaQueryWrapper<BusShift> wrapper = new LambdaQueryWrapper<BusShift>();
        if (date != null) {
            wrapper.eq(BusShift::getShiftDate, date);
        }
        if (routeId != null) {
            wrapper.eq(BusShift::getRouteId, routeId);
        }
        // 状态优先级：运行中 → 待发车 → 已完成 → 已取消，同状态按发车时间升序
        wrapper.last("ORDER BY FIELD(status, 'RUNNING', 'SCHEDULED', 'FINISHED', 'CANCELLED'), shift_date ASC, depart_time ASC");
        return buildVOs(shiftMapper.selectList(wrapper));
    }

    public ShiftVO getVO(Long id) {
        BusShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        return buildVOs(Collections.singletonList(shift)).get(0);
    }

    public ShiftDetailVO detail(Long id) {
        ShiftVO vo = getVO(id);
        ShiftDetailVO detail = new ShiftDetailVO();
        BeanUtils.copyProperties(vo, detail);
        detail.setStops(stopMapper.selectList(new LambdaQueryWrapper<BusStop>()
                .eq(BusStop::getRouteId, vo.getRouteId())
                .orderByAsc(BusStop::getSeq)));
        return detail;
    }

    public List<ShiftVO> buildVOs(List<BusShift> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> routeIds = shifts.stream().map(BusShift::getRouteId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> vehicleIds = shifts.stream().map(BusShift::getVehicleId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> driverIds = shifts.stream().map(BusShift::getDriverId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> shiftIds = shifts.stream().map(BusShift::getId).collect(Collectors.toList());

        Map<Long, BusRoute> routeMap = routeIds.isEmpty() ? Collections.emptyMap()
                : routeMapper.selectBatchIds(routeIds).stream()
                .collect(Collectors.toMap(BusRoute::getId, Function.identity()));
        Map<Long, BusVehicle> vehicleMap = vehicleIds.isEmpty() ? Collections.emptyMap()
                : vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(BusVehicle::getId, Function.identity()));
        Map<Long, SysUser> driverMap = driverIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(driverIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, List<BusStop>> stopsMap = routeIds.isEmpty() ? Collections.emptyMap()
                : stopMapper.selectList(new LambdaQueryWrapper<BusStop>()
                        .in(BusStop::getRouteId, routeIds)
                        .orderByAsc(BusStop::getSeq))
                .stream().collect(Collectors.groupingBy(BusStop::getRouteId));
        Map<Long, Long> reservedMap = reservationMapper.selectList(new LambdaQueryWrapper<BusReservation>()
                        .in(BusReservation::getShiftId, shiftIds)
                        .in(BusReservation::getStatus, ReservationStatus.RESERVED, ReservationStatus.SIGNED))
                .stream().collect(Collectors.groupingBy(BusReservation::getShiftId, Collectors.counting()));

        return shifts.stream().map(shift -> {
            ShiftVO vo = new ShiftVO();
            vo.setId(shift.getId());
            vo.setRouteId(shift.getRouteId());
            BusRoute route = routeMap.get(shift.getRouteId());
            vo.setRouteName(route == null ? "未知线路" : route.getName());
            vo.setVehicleId(shift.getVehicleId());
            BusVehicle vehicle = shift.getVehicleId() == null ? null : vehicleMap.get(shift.getVehicleId());
            vo.setVehiclePlate(vehicle == null ? "未分配" : vehicle.getPlateNo());
            vo.setVehicleModel(vehicle == null ? "" : vehicle.getModel());
            int capacity = vehicle == null || vehicle.getCapacity() == null ? 0 : vehicle.getCapacity();
            vo.setCapacity(capacity);
            vo.setDriverId(shift.getDriverId());
            SysUser driver = shift.getDriverId() == null ? null : driverMap.get(shift.getDriverId());
            vo.setDriverName(driver == null ? "未分配" : driver.getName());
            vo.setShiftDate(shift.getShiftDate());
            vo.setDepartTime(shift.getDepartTime());
            vo.setStatus(shift.getStatus());
            int reserved = Math.toIntExact(reservedMap.getOrDefault(shift.getId(), 0L));
            vo.setReservedCount(reserved);
            vo.setRemainingSeats(Math.max(0, capacity - reserved));
            List<BusStop> stops = stopsMap.getOrDefault(shift.getRouteId(), Collections.emptyList());
            vo.setStopCount(stops.size());
            vo.setStartStopName(stops.isEmpty() ? "" : stops.get(0).getName());
            vo.setEndStopName(stops.isEmpty() ? "" : stops.get(stops.size() - 1).getName());
            return vo;
        }).collect(Collectors.toList());
    }

    public ShiftDetailVO create(BusShift shift) {
        shift.setId(null);
        shift.setStatus(shift.getStatus() == null ? ShiftStatus.SCHEDULED : shift.getStatus());
        validate(shift, null);
        shift.setCreateTime(LocalDateTime.now());
        shiftMapper.insert(shift);
        syncTask(shift);
        return detail(shift.getId());
    }

    public ShiftDetailVO update(Long id, BusShift payload) {
        BusShift exists = shiftMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("班次不存在");
        }
        if (ShiftStatus.FINISHED.equals(exists.getStatus())) {
            throw new BusinessException("已完成的班次不能修改");
        }
        payload.setId(id);
        payload.setStatus(payload.getStatus() == null ? exists.getStatus() : payload.getStatus());
        validate(payload, id);
        int reserved = activeReservedCount(id);
        if (reserved > 0 && payload.getVehicleId() == null) {
            throw new BusinessException("该班次已有预约，不能取消车辆分配");
        }
        Long newVehicleId = payload.getVehicleId();
        if (reserved > 0 && newVehicleId != null) {
            BusVehicle vehicle = vehicleMapper.selectById(newVehicleId);
            if (vehicle == null || vehicle.getCapacity() < reserved) {
                throw new BusinessException("新车辆容量不足以容纳已有预约人数（" + reserved + "）");
            }
        }
        if (ShiftStatus.CANCELLED.equals(payload.getStatus())) {
            DriverTask task = findTask(id);
            if (task != null && TaskStatus.RUNNING.equals(task.getStatus())) {
                throw new BusinessException("司机正在执行该班次，不能取消");
            }
        }
        exists.setRouteId(payload.getRouteId());
        exists.setVehicleId(payload.getVehicleId());
        exists.setDriverId(payload.getDriverId());
        exists.setShiftDate(payload.getShiftDate());
        exists.setDepartTime(payload.getDepartTime());
        exists.setStatus(payload.getStatus());
        shiftMapper.updateById(exists);
        syncTask(exists);
        return detail(id);
    }

    public ShiftDetailVO assign(Long id, AssignRequest request) {
        BusShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        if (ShiftStatus.FINISHED.equals(shift.getStatus()) || ShiftStatus.CANCELLED.equals(shift.getStatus())) {
            throw new BusinessException("当前班次状态不允许调整车辆");
        }
        if (ShiftStatus.RUNNING.equals(shift.getStatus())) {
            throw new BusinessException("班次正在运行，不能调整车辆或司机");
        }
        BusVehicle vehicle = vehicleMapper.selectById(request.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        if (VehicleStatus.DISABLED.equals(vehicle.getStatus())) {
            throw new BusinessException("该车辆已停用");
        }
        int reserved = activeReservedCount(id);
        if (vehicle.getCapacity() < reserved) {
            throw new BusinessException("车辆容量不足以容纳已有预约人数（" + reserved + "）");
        }
        SysUser driver = userMapper.selectById(request.getDriverId());
        if (driver == null || !Roles.DRIVER.equals(driver.getRole()) || driver.getStatus() != 1) {
            throw new BusinessException("请选择有效的司机");
        }
        shift.setVehicleId(request.getVehicleId());
        shift.setDriverId(request.getDriverId());
        shiftMapper.updateById(shift);
        syncTask(shift);
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        BusShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        if (activeReservedCount(id) > 0) {
            throw new BusinessException("该班次已有学生预约，不能删除");
        }
        DriverTask task = findTask(id);
        if (task != null && TaskStatus.RUNNING.equals(task.getStatus())) {
            throw new BusinessException("司机正在执行该班次，不能删除");
        }
        taskMapper.delete(new LambdaQueryWrapper<DriverTask>().eq(DriverTask::getShiftId, id));
        locationMapper.delete(new LambdaQueryWrapper<BusVehicleLocation>()
                .eq(BusVehicleLocation::getShiftId, id));
        shiftMapper.deleteById(id);
    }

    public List<SysUser> listDrivers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Roles.DRIVER)
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
    }

    public int activeReservedCount(Long shiftId) {
        Long count = reservationMapper.selectCount(new LambdaQueryWrapper<BusReservation>()
                .eq(BusReservation::getShiftId, shiftId)
                .in(BusReservation::getStatus, ReservationStatus.RESERVED, ReservationStatus.SIGNED));
        return count == null ? 0 : count.intValue();
    }

    private void validate(BusShift shift, Long excludeId) {
        if (shift.getRouteId() == null) {
            throw new BusinessException("请选择线路");
        }
        if (routeMapper.selectById(shift.getRouteId()) == null) {
            throw new BusinessException("线路不存在");
        }
        if (shift.getShiftDate() == null) {
            throw new BusinessException("请选择班次日期");
        }
        if (shift.getDepartTime() == null) {
            throw new BusinessException("请选择发车时间");
        }
        if (shift.getVehicleId() != null) {
            BusVehicle vehicle = vehicleMapper.selectById(shift.getVehicleId());
            if (vehicle == null) {
                throw new BusinessException("车辆不存在");
            }
            if (VehicleStatus.DISABLED.equals(vehicle.getStatus())) {
                throw new BusinessException("不能分配已停用的车辆");
            }
        }
        if (shift.getDriverId() != null) {
            SysUser driver = userMapper.selectById(shift.getDriverId());
            if (driver == null || !Roles.DRIVER.equals(driver.getRole())) {
                throw new BusinessException("司机账号不存在");
            }
        }
        if (shift.getStatus() != null
                && !List.of(ShiftStatus.SCHEDULED, ShiftStatus.RUNNING,
                ShiftStatus.FINISHED, ShiftStatus.CANCELLED).contains(shift.getStatus())) {
            throw new BusinessException("班次状态不合法");
        }
        LambdaQueryWrapper<BusShift> duplicate = new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getRouteId, shift.getRouteId())
                .eq(BusShift::getShiftDate, shift.getShiftDate())
                .eq(BusShift::getDepartTime, shift.getDepartTime());
        if (excludeId != null) {
            duplicate.ne(BusShift::getId, excludeId);
        }
        Long count = shiftMapper.selectCount(duplicate);
        if (count != null && count > 0) {
            throw new BusinessException("同一线路在该日期与时间已存在班次");
        }
    }

    private DriverTask findTask(Long shiftId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<DriverTask>()
                .eq(DriverTask::getShiftId, shiftId));
    }

    private void syncTask(BusShift shift) {
        DriverTask task = findTask(shift.getId());
        if (shift.getDriverId() == null || ShiftStatus.CANCELLED.equals(shift.getStatus())) {
            if (task != null && !TaskStatus.RUNNING.equals(task.getStatus())) {
                taskMapper.deleteById(task.getId());
            }
            return;
        }
        if (task == null) {
            task = new DriverTask();
            task.setDriverId(shift.getDriverId());
            task.setShiftId(shift.getId());
            task.setVehicleId(shift.getVehicleId());
            task.setStatus(TaskStatus.PENDING);
            task.setCreateTime(LocalDateTime.now());
            taskMapper.insert(task);
        } else {
            boolean assignmentChanged = !Objects.equals(task.getDriverId(), shift.getDriverId())
                    || !Objects.equals(task.getVehicleId(), shift.getVehicleId());
            task.setDriverId(shift.getDriverId());
            task.setVehicleId(shift.getVehicleId());
            if (TaskStatus.FINISHED.equals(task.getStatus()) && assignmentChanged) {
                task.setStatus(TaskStatus.PENDING);
                task.setStartTime(null);
                task.setEndTime(null);
            }
            taskMapper.updateById(task);
        }
    }

    /**
     * 按"前一天有效班次"为模板，为指定日期自动发布新班次。
     * <p>
     * 每天凌晨调度任务调用此方法（默认 target = 今天）。
     * 重复调度是幂等的：同一 (route_id, shift_date, depart_time) 三元组
     * 当天已存在则跳过，不会产生脏数据。
     *
     * @param target 要发布的班次日期；null 表示今天
     * @return 本次新创建的班次数量
     */
    @Transactional
    public int publishDaily(LocalDate target) {
        LocalDate targetDate = target == null ? LocalDate.now() : target;
        LocalDate templateDate = targetDate.minusDays(1);

        // 模板：前一天所有未取消的班次（RUNNING / SCHEDULED / FINISHED 都能作为排班模板）
        List<BusShift> templates = shiftMapper.selectList(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getShiftDate, templateDate)
                .ne(BusShift::getStatus, ShiftStatus.CANCELLED));
        if (templates.isEmpty()) {
            return 0;
        }

        // target 日期已存在的 (route_id, depart_time) 集合，用于幂等跳过
        List<BusShift> existing = shiftMapper.selectList(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getShiftDate, targetDate));
        Set<String> existingKey = existing.stream()
                .map(s -> s.getRouteId() + "_" + s.getDepartTime())
                .collect(Collectors.toSet());

        int created = 0;
        for (BusShift tpl : templates) {
            String key = tpl.getRouteId() + "_" + tpl.getDepartTime();
            if (existingKey.contains(key)) {
                continue;
            }
            BusShift shift = new BusShift();
            shift.setRouteId(tpl.getRouteId());
            shift.setVehicleId(tpl.getVehicleId());
            shift.setDriverId(tpl.getDriverId());
            shift.setShiftDate(targetDate);
            shift.setDepartTime(tpl.getDepartTime());
            shift.setStatus(ShiftStatus.SCHEDULED);
            shift.setCreateTime(LocalDateTime.now());
            shiftMapper.insert(shift);
            syncTask(shift);
            created++;
        }
        return created;
    }
}
