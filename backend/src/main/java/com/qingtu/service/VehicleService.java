package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.ReservationStatus;
import com.qingtu.common.VehicleStatus;
import com.qingtu.entity.BusReservation;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusVehicle;
import com.qingtu.mapper.BusReservationMapper;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusVehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final BusVehicleMapper vehicleMapper;
    private final BusShiftMapper shiftMapper;
    private final BusReservationMapper reservationMapper;

    public List<BusVehicle> list(String status) {
        LambdaQueryWrapper<BusVehicle> wrapper = new LambdaQueryWrapper<BusVehicle>()
                .orderByAsc(BusVehicle::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(BusVehicle::getStatus, status);
        }
        return vehicleMapper.selectList(wrapper);
    }

    public BusVehicle create(BusVehicle vehicle) {
        validate(vehicle);
        Long exists = vehicleMapper.selectCount(new LambdaQueryWrapper<BusVehicle>()
                .eq(BusVehicle::getPlateNo, vehicle.getPlateNo()));
        if (exists != null && exists > 0) {
            throw new BusinessException("车辆编号已存在");
        }
        vehicle.setId(null);
        vehicle.setStatus(vehicle.getStatus() == null ? VehicleStatus.AVAILABLE : vehicle.getStatus());
        vehicle.setCreateTime(LocalDateTime.now());
        vehicleMapper.insert(vehicle);
        return vehicle;
    }

    public BusVehicle update(Long id, BusVehicle vehicle) {
        BusVehicle exists = vehicleMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("车辆不存在");
        }
        validate(vehicle);
        Long samePlate = vehicleMapper.selectCount(new LambdaQueryWrapper<BusVehicle>()
                .eq(BusVehicle::getPlateNo, vehicle.getPlateNo())
                .ne(BusVehicle::getId, id));
        if (samePlate != null && samePlate > 0) {
            throw new BusinessException("车辆编号已存在");
        }
        int maxReserved = maxReserved(id);
        if (vehicle.getCapacity() < maxReserved) {
            throw new BusinessException("车辆容量不能小于该车已分配班次的最大预约人数（" + maxReserved + "）");
        }
        exists.setPlateNo(vehicle.getPlateNo());
        exists.setModel(vehicle.getModel());
        exists.setCapacity(vehicle.getCapacity());
        exists.setStatus(vehicle.getStatus());
        vehicleMapper.updateById(exists);
        return exists;
    }

    public void delete(Long id) {
        if (vehicleMapper.selectById(id) == null) {
            throw new BusinessException("车辆不存在");
        }
        Long shiftCount = shiftMapper.selectCount(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getVehicleId, id));
        if (shiftCount != null && shiftCount > 0) {
            throw new BusinessException("车辆已被班次使用，不能删除，可将状态改为停用");
        }
        vehicleMapper.deleteById(id);
    }

    public void setStatus(Long id, String status) {
        BusVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        vehicle.setStatus(status);
        vehicleMapper.updateById(vehicle);
    }

    private void validate(BusVehicle vehicle) {
        if (vehicle.getPlateNo() == null || vehicle.getPlateNo().isBlank()) {
            throw new BusinessException("请输入车辆编号");
        }
        if (vehicle.getModel() == null || vehicle.getModel().isBlank()) {
            throw new BusinessException("请输入车型");
        }
        if (vehicle.getCapacity() == null || vehicle.getCapacity() < 1) {
            throw new BusinessException("车辆容量必须大于 0");
        }
        if (vehicle.getStatus() != null
                && !List.of(VehicleStatus.AVAILABLE, VehicleStatus.RUNNING, VehicleStatus.DISABLED)
                .contains(vehicle.getStatus())) {
            throw new BusinessException("车辆状态不合法");
        }
    }

    private int maxReserved(Long vehicleId) {
        List<BusShift> shifts = shiftMapper.selectList(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getVehicleId, vehicleId));
        if (shifts.isEmpty()) {
            return 0;
        }
        List<Long> shiftIds = shifts.stream().map(BusShift::getId).collect(Collectors.toList());
        List<BusReservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<BusReservation>()
                .in(BusReservation::getShiftId, shiftIds)
                .in(BusReservation::getStatus, ReservationStatus.RESERVED, ReservationStatus.SIGNED));
        return shifts.stream()
                .mapToInt(s -> (int) reservations.stream()
                        .filter(r -> r.getShiftId().equals(s.getId()))
                        .count())
                .max()
                .orElse(0);
    }
}
