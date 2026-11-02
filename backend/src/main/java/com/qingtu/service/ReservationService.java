package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.ReservationStatus;
import com.qingtu.common.ShiftStatus;
import com.qingtu.common.VehicleStatus;
import com.qingtu.dto.ReservationVO;
import com.qingtu.dto.ShiftVO;
import com.qingtu.entity.BusReservation;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusVehicle;
import com.qingtu.entity.SysUser;
import com.qingtu.mapper.BusReservationMapper;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusVehicleMapper;
import com.qingtu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final BusReservationMapper reservationMapper;
    private final BusShiftMapper shiftMapper;
    private final BusVehicleMapper vehicleMapper;
    private final SysUserMapper userMapper;
    private final ShiftService shiftService;

    @Transactional
    public ReservationVO create(Long userId, Long shiftId) {
        BusShift shift = shiftMapper.selectForUpdate(shiftId);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        if (ShiftStatus.CANCELLED.equals(shift.getStatus()) || ShiftStatus.FINISHED.equals(shift.getStatus())) {
            throw new BusinessException("该班次当前不可预约");
        }
        if (shift.getShiftDate().isBefore(LocalDate.now())) {
            throw new BusinessException("不能预约历史班次");
        }
        if (shift.getVehicleId() == null) {
            throw new BusinessException("该班次尚未分配车辆，暂不可预约");
        }
        BusVehicle vehicle = vehicleMapper.selectById(shift.getVehicleId());
        if (vehicle == null || VehicleStatus.DISABLED.equals(vehicle.getStatus())) {
            throw new BusinessException("该班次车辆不可用，暂不可预约");
        }
        Long duplicated = reservationMapper.selectCount(new LambdaQueryWrapper<BusReservation>()
                .eq(BusReservation::getUserId, userId)
                .eq(BusReservation::getShiftId, shiftId)
                .in(BusReservation::getStatus, ReservationStatus.RESERVED, ReservationStatus.SIGNED));
        if (duplicated != null && duplicated > 0) {
            throw new BusinessException("您已预约该班次，请勿重复预约");
        }
        int reserved = shiftService.activeReservedCount(shiftId);
        if (reserved >= vehicle.getCapacity()) {
            throw new BusinessException("该班次座位已满");
        }
        BusReservation reservation = new BusReservation();
        reservation.setUserId(userId);
        reservation.setShiftId(shiftId);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreateTime(LocalDateTime.now());
        reservationMapper.insert(reservation);
        return toVO(reservation, shiftService.getVO(shiftId), findUserName(userId));
    }

    public List<ReservationVO> listByUser(Long userId) {
        List<BusReservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<BusReservation>()
                        .eq(BusReservation::getUserId, userId)
                        .orderByDesc(BusReservation::getCreateTime));
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }
        List<BusShift> shifts = shiftMapper.selectBatchIds(reservations.stream()
                .map(BusReservation::getShiftId).collect(Collectors.toSet()));
        Map<Long, ShiftVO> shiftMap = shiftService.buildVOs(shifts).stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));
        String name = findUserName(userId);
        return reservations.stream()
                .map(r -> toVO(r, shiftMap.get(r.getShiftId()), name))
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationVO cancel(Long userId, Long reservationId) {
        BusReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!Objects.equals(reservation.getUserId(), userId)) {
            throw new BusinessException(403, "只能取消自己的预约");
        }
        if (!ReservationStatus.RESERVED.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已取消或已签到，无法取消");
        }
        BusShift shift = shiftMapper.selectById(reservation.getShiftId());
        if (shift != null && (ShiftStatus.RUNNING.equals(shift.getStatus())
                || ShiftStatus.FINISHED.equals(shift.getStatus()))) {
            throw new BusinessException("车辆已开始运行，无法取消预约");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        return toVO(reservation, shift == null ? null : shiftService.getVO(shift.getId()), findUserName(userId));
    }

    /**
     * 学生签到：将本人该预约状态由 RESERVED 更新为 SIGNED。
     */
    @Transactional
    public ReservationVO sign(Long userId, Long reservationId) {
        BusReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!Objects.equals(reservation.getUserId(), userId)) {
            throw new BusinessException(403, "只能签到自己名下的预约");
        }
        if (ReservationStatus.SIGNED.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已签到，请勿重复签到");
        }
        if (!ReservationStatus.RESERVED.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已取消，无法签到");
        }
        BusShift shift = shiftMapper.selectById(reservation.getShiftId());
        if (shift != null && (ShiftStatus.FINISHED.equals(shift.getStatus())
                || ShiftStatus.CANCELLED.equals(shift.getStatus()))) {
            throw new BusinessException("该班次已结束或已取消，无法签到");
        }
        reservation.setStatus(ReservationStatus.SIGNED);
        reservationMapper.updateById(reservation);
        return toVO(reservation, shift == null ? null : shiftService.getVO(shift.getId()), findUserName(userId));
    }

    /**
     * 查询学生在某班次的有效预约（未取消），用于班次详情页展示签到入口。
     */
    public BusReservation findActiveReservation(Long userId, Long shiftId) {
        List<BusReservation> list = reservationMapper.selectList(new LambdaQueryWrapper<BusReservation>()
                .eq(BusReservation::getUserId, userId)
                .eq(BusReservation::getShiftId, shiftId)
                .in(BusReservation::getStatus, ReservationStatus.RESERVED, ReservationStatus.SIGNED)
                .orderByDesc(BusReservation::getCreateTime));
        return list.isEmpty() ? null : list.get(0);
    }

    public List<ReservationVO> listAll(Long shiftId, LocalDate date) {
        LambdaQueryWrapper<BusReservation> wrapper = new LambdaQueryWrapper<BusReservation>()
                .orderByDesc(BusReservation::getCreateTime);
        if (shiftId != null) {
            wrapper.eq(BusReservation::getShiftId, shiftId);
        }
        List<BusReservation> reservations = reservationMapper.selectList(wrapper);
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }
        List<BusShift> shifts = shiftMapper.selectBatchIds(reservations.stream()
                .map(BusReservation::getShiftId).collect(Collectors.toSet()));
        if (date != null) {
            shifts = shifts.stream().filter(s -> date.equals(s.getShiftDate())).collect(Collectors.toList());
            List<Long> shiftIds = shifts.stream().map(BusShift::getId).collect(Collectors.toList());
            reservations = reservations.stream()
                    .filter(r -> shiftIds.contains(r.getShiftId()))
                    .collect(Collectors.toList());
        }
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ShiftVO> shiftMap = shiftService.buildVOs(shifts).stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));
        List<Long> userIds = reservations.stream().map(BusReservation::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return reservations.stream()
                .map(r -> {
                    SysUser user = userMap.get(r.getUserId());
                    return toVO(r, shiftMap.get(r.getShiftId()), user == null ? "" : user.getName());
                })
                .collect(Collectors.toList());
    }

    private String findUserName(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? "" : user.getName();
    }

    private ReservationVO toVO(BusReservation reservation, ShiftVO shift, String studentName) {
        ReservationVO vo = new ReservationVO();
        vo.setId(reservation.getId());
        vo.setShiftId(reservation.getShiftId());
        vo.setStudentName(studentName);
        vo.setStatus(reservation.getStatus());
        vo.setCreateTime(reservation.getCreateTime());
        vo.setCancelTime(reservation.getCancelTime());
        if (shift != null) {
            vo.setRouteId(shift.getRouteId());
            vo.setRouteName(shift.getRouteName());
            vo.setShiftDate(shift.getShiftDate());
            vo.setDepartTime(shift.getDepartTime());
            vo.setVehiclePlate(shift.getVehiclePlate());
            vo.setCapacity(shift.getCapacity());
            vo.setReservedCount(shift.getReservedCount());
            vo.setRemainingSeats(shift.getRemainingSeats());
            vo.setShiftStatus(shift.getStatus());
        }
        return vo;
    }
}
