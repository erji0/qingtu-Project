package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.RunStatus;
import com.qingtu.common.ShiftStatus;
import com.qingtu.common.TaskStatus;
import com.qingtu.common.VehicleStatus;
import com.qingtu.dto.LocationVO;
import com.qingtu.dto.ShiftVO;
import com.qingtu.dto.TaskVO;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusStop;
import com.qingtu.entity.BusVehicle;
import com.qingtu.entity.DriverTask;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusStopMapper;
import com.qingtu.mapper.BusVehicleMapper;
import com.qingtu.mapper.DriverTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final DriverTaskMapper taskMapper;
    private final BusShiftMapper shiftMapper;
    private final BusVehicleMapper vehicleMapper;
    private final BusStopMapper stopMapper;
    private final ShiftService shiftService;
    private final LocationService locationService;

    public List<TaskVO> listByDriver(Long driverId) {
        List<DriverTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DriverTask>()
                .eq(DriverTask::getDriverId, driverId));
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ShiftVO> shiftMap = shiftService.buildVOs(
                        shiftMapper.selectBatchIds(tasks.stream()
                                .map(DriverTask::getShiftId).collect(Collectors.toSet())))
                .stream().collect(Collectors.toMap(ShiftVO::getId, Function.identity()));
        return tasks.stream()
                .map(task -> buildVO(task, shiftMap.get(task.getShiftId()), false))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt((TaskVO t) -> statusPriority(t.getStatus()))
                        .thenComparing(TaskVO::getShiftDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TaskVO::getDepartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public TaskVO detail(Long taskId, Long driverId) {
        DriverTask task = requireOwnTask(taskId, driverId);
        return buildVO(task, shiftService.getVO(task.getShiftId()), true);
    }

    @Transactional
    public TaskVO start(Long taskId, Long driverId) {
        DriverTask task = requireOwnTask(taskId, driverId);
        if (TaskStatus.FINISHED.equals(task.getStatus())) {
            throw new BusinessException("该任务已完成");
        }
        if (TaskStatus.RUNNING.equals(task.getStatus())) {
            throw new BusinessException("该任务已开始运行");
        }
        BusShift shift = shiftMapper.selectById(task.getShiftId());
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        if (ShiftStatus.CANCELLED.equals(shift.getStatus()) || ShiftStatus.FINISHED.equals(shift.getStatus())) {
            throw new BusinessException("该班次当前不可执行");
        }
        if (shift.getVehicleId() == null) {
            throw new BusinessException("该班次尚未分配车辆，请联系管理员");
        }
        task.setStatus(TaskStatus.RUNNING);
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(null);
        taskMapper.updateById(task);

        shift.setStatus(ShiftStatus.RUNNING);
        shiftMapper.updateById(shift);

        BusVehicle vehicle = vehicleMapper.selectById(shift.getVehicleId());
        if (vehicle != null && !VehicleStatus.DISABLED.equals(vehicle.getStatus())) {
            vehicle.setStatus(VehicleStatus.RUNNING);
            vehicleMapper.updateById(vehicle);
        }
        locationService.initLocation(shift);
        return detail(taskId, driverId);
    }

    @Transactional
    public TaskVO finish(Long taskId, Long driverId) {
        DriverTask task = requireOwnTask(taskId, driverId);
        if (!TaskStatus.RUNNING.equals(task.getStatus())) {
            throw new BusinessException("只有执行中的任务才能结束");
        }
        task.setStatus(TaskStatus.FINISHED);
        task.setEndTime(LocalDateTime.now());
        taskMapper.updateById(task);

        BusShift shift = shiftMapper.selectById(task.getShiftId());
        if (shift != null) {
            shift.setStatus(ShiftStatus.FINISHED);
            shiftMapper.updateById(shift);
            BusVehicle vehicle = shift.getVehicleId() == null ? null : vehicleMapper.selectById(shift.getVehicleId());
            if (vehicle != null && !VehicleStatus.DISABLED.equals(vehicle.getStatus())) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleMapper.updateById(vehicle);
            }
        }
        locationService.finishLocation(task.getShiftId());
        return detail(taskId, driverId);
    }

    public LocationVO updateRunStatus(Long taskId, Long driverId, String runStatus) {
        DriverTask task = requireOwnTask(taskId, driverId);
        if (!TaskStatus.RUNNING.equals(task.getStatus())) {
            throw new BusinessException("任务尚未开始运行");
        }
        return locationService.updateRunStatus(task.getShiftId(), runStatus);
    }

    public LocationVO location(Long taskId, Long driverId) {
        DriverTask task = requireOwnTask(taskId, driverId);
        return locationService.getByShift(task.getShiftId());
    }

    private DriverTask requireOwnTask(Long taskId, Long driverId) {
        DriverTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!Objects.equals(task.getDriverId(), driverId)) {
            throw new BusinessException(403, "只能查看分配给自己的任务");
        }
        return task;
    }

    private TaskVO buildVO(DriverTask task, ShiftVO shift, boolean withLocation) {
        if (shift == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setShiftId(task.getShiftId());
        vo.setRouteId(shift.getRouteId());
        vo.setRouteName(shift.getRouteName());
        vo.setShiftDate(shift.getShiftDate());
        vo.setDepartTime(shift.getDepartTime());
        vo.setVehicleId(shift.getVehicleId());
        vo.setPlateNo(shift.getVehiclePlate());
        vo.setCapacity(shift.getCapacity());
        vo.setReservedCount(shift.getReservedCount());
        vo.setRemainingSeats(shift.getRemainingSeats());
        vo.setStatus(task.getStatus());
        vo.setShiftStatus(shift.getStatus());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        List<BusStop> stops = stopMapper.selectList(new LambdaQueryWrapper<BusStop>()
                .eq(BusStop::getRouteId, shift.getRouteId())
                .orderByAsc(BusStop::getSeq));
        vo.setStops(stops);
        if (withLocation) {
            vo.setLocation(locationService.getByShift(task.getShiftId()));
        }
        return vo;
    }

    private int statusPriority(String status) {
        if (TaskStatus.RUNNING.equals(status)) {
            return 0;
        }
        if (TaskStatus.PENDING.equals(status)) {
            return 1;
        }
        return 2;
    }
}
