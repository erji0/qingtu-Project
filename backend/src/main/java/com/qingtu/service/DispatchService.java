package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.ShiftStatus;
import com.qingtu.common.SuggestionStatus;
import com.qingtu.common.SuggestionType;
import com.qingtu.common.TaskStatus;
import com.qingtu.common.VehicleStatus;
import com.qingtu.dto.ShiftVO;
import com.qingtu.dto.SuggestionVO;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusVehicle;
import com.qingtu.entity.DispatchSuggestion;
import com.qingtu.entity.DriverTask;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusVehicleMapper;
import com.qingtu.mapper.DispatchSuggestionMapper;
import com.qingtu.mapper.DriverTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final DispatchSuggestionMapper suggestionMapper;
    private final BusShiftMapper shiftMapper;
    private final BusVehicleMapper vehicleMapper;
    private final DriverTaskMapper taskMapper;
    private final ShiftService shiftService;

    @Transactional
    public int generate(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<ShiftVO> shifts = shiftService.list(target, null).stream()
                .filter(s -> !ShiftStatus.CANCELLED.equals(s.getStatus())
                        && !ShiftStatus.FINISHED.equals(s.getStatus()))
                .collect(Collectors.toList());
        if (shifts.isEmpty()) {
            return 0;
        }
        List<BusVehicle> candidates = vehicleMapper.selectList(new LambdaQueryWrapper<BusVehicle>()
                .eq(BusVehicle::getStatus, VehicleStatus.AVAILABLE));
        Set<String> pendingKeys = suggestionMapper.selectList(new LambdaQueryWrapper<DispatchSuggestion>()
                        .eq(DispatchSuggestion::getStatus, SuggestionStatus.PENDING))
                .stream()
                .map(s -> s.getShiftId() + "|" + s.getType())
                .collect(Collectors.toSet());

        int created = 0;
        Map<Long, Double> rateMap = new HashMap<>();
        Map<Long, ShiftVO> shiftMap = shifts.stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));

        for (ShiftVO shift : shifts) {
            int reserved = shift.getReservedCount() == null ? 0 : shift.getReservedCount();
            int capacity = shift.getCapacity() == null ? 0 : shift.getCapacity();
            double rate = capacity == 0 ? 0 : reserved * 1.0 / capacity;
            rateMap.put(shift.getId(), rate);

            BusVehicle current = shift.getVehicleId() == null ? null
                    : vehicleMapper.selectById(shift.getVehicleId());
            if (current == null || VehicleStatus.DISABLED.equals(current.getStatus())) {
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, 1), false, null);
                String reason = current == null
                        ? "班次尚未分配车辆，建议重新分配可用车辆"
                        : "原车辆 " + current.getPlateNo() + " 已停用，建议重新分配可用车辆";
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.REASSIGN_VEHICLE, reason);
                continue;
            }

            if (rate > 0.95) {
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, capacity + 1), false, current.getId());
                String reason = String.format("预约率 %.1f%% 已超过 95%%，建议增加车辆或更换更大容量车辆", rate * 100);
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.ADD_VEHICLE, reason);
            } else if (rate >= 0.80) {
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, capacity + 1), false, current.getId());
                String reason = String.format("预约率 %.1f%% 处于 80%%-95%%，建议更换更大容量车辆", rate * 100);
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.LARGER_VEHICLE, reason);
            } else if (rate < 0.30) {
                BusVehicle recommended = chooseSmallerCandidate(candidates, reserved, capacity, current.getId());
                String reason = String.format("预约率 %.1f%% 低于 30%%，建议调整车辆（小容量）或合并班次", rate * 100);
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.REDUCE_CAPACITY, reason);
            }
        }

        // 同一线路相邻班次预约率差异超过 40%，提示运力重新分配
        Map<Long, List<ShiftVO>> byRoute = shifts.stream()
                .collect(Collectors.groupingBy(ShiftVO::getRouteId));
        for (List<ShiftVO> routeShifts : byRoute.values()) {
            routeShifts.sort(Comparator.comparing(ShiftVO::getDepartTime));
            for (int i = 1; i < routeShifts.size(); i++) {
                ShiftVO previous = routeShifts.get(i - 1);
                ShiftVO current = routeShifts.get(i);
                double prevRate = rateMap.getOrDefault(previous.getId(), 0.0);
                double currRate = rateMap.getOrDefault(current.getId(), 0.0);
                if (Math.abs(prevRate - currRate) > 0.40) {
                    ShiftVO lower = prevRate < currRate ? previous : current;
                    ShiftVO higher = prevRate < currRate ? current : previous;
                    String reason = String.format("相邻班次预约率差异过大（%.1f%% vs %.1f%%），建议将运力向高需求班次重新分配",
                            prevRate * 100, currRate * 100);
                    BusVehicle currentVehicle = lower.getVehicleId() == null ? null
                            : vehicleMapper.selectById(lower.getVehicleId());
                    created += createIfAbsent(pendingKeys, lower, currentVehicle, null,
                            SuggestionType.REALLOCATE, reason);
                }
            }
        }
        return created;
    }

    public List<SuggestionVO> list(String status, LocalDate date) {
        LambdaQueryWrapper<DispatchSuggestion> wrapper = new LambdaQueryWrapper<DispatchSuggestion>()
                .orderByDesc(DispatchSuggestion::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(DispatchSuggestion::getStatus, status);
        }
        List<DispatchSuggestion> suggestions = suggestionMapper.selectList(wrapper);
        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> shiftIds = suggestions.stream().map(DispatchSuggestion::getShiftId)
                .collect(Collectors.toSet());
        List<BusShift> shifts = shiftMapper.selectBatchIds(shiftIds);
        if (date != null) {
            List<Long> dateShiftIds = shifts.stream()
                    .filter(s -> date.equals(s.getShiftDate()))
                    .map(BusShift::getId).collect(Collectors.toList());
            suggestions = suggestions.stream()
                    .filter(s -> dateShiftIds.contains(s.getShiftId()))
                    .collect(Collectors.toList());
            if (suggestions.isEmpty()) {
                return Collections.emptyList();
            }
        }
        Map<Long, ShiftVO> shiftMap = shiftService.buildVOs(shifts).stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));
        Set<Long> vehicleIds = new HashSet<>();
        suggestions.forEach(s -> {
            if (s.getVehicleId() != null) {
                vehicleIds.add(s.getVehicleId());
            }
            if (s.getSuggestedVehicleId() != null) {
                vehicleIds.add(s.getSuggestedVehicleId());
            }
        });
        Map<Long, BusVehicle> vehicleMap = vehicleIds.isEmpty() ? Collections.emptyMap()
                : vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(BusVehicle::getId, Function.identity()));

        return suggestions.stream()
                .map(s -> toVO(s, shiftMap.get(s.getShiftId()), vehicleMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirm(Long id) {
        DispatchSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null) {
            throw new BusinessException("调度建议不存在");
        }
        if (!SuggestionStatus.PENDING.equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已处理，请勿重复操作");
        }
        BusShift shift = shiftMapper.selectById(suggestion.getShiftId());
        if (shift == null) {
            throw new BusinessException("关联班次不存在");
        }
        if (suggestion.getSuggestedVehicleId() != null) {
            BusVehicle suggested = vehicleMapper.selectById(suggestion.getSuggestedVehicleId());
            if (suggested == null) {
                throw new BusinessException("推荐车辆不存在");
            }
            if (VehicleStatus.DISABLED.equals(suggested.getStatus())) {
                throw new BusinessException("推荐车辆已停用，请改用其他车辆");
            }
            int reserved = shiftService.activeReservedCount(shift.getId());
            if (suggested.getCapacity() < reserved) {
                throw new BusinessException("推荐车辆容量不足以容纳已有预约人数");
            }
            Long oldVehicleId = shift.getVehicleId();
            shift.setVehicleId(suggested.getId());
            shiftMapper.updateById(shift);
            if (oldVehicleId != null && !oldVehicleId.equals(suggested.getId())) {
                BusVehicle old = vehicleMapper.selectById(oldVehicleId);
                if (old != null && !VehicleStatus.DISABLED.equals(old.getStatus())) {
                    old.setStatus(VehicleStatus.AVAILABLE);
                    vehicleMapper.updateById(old);
                }
            }
            DriverTask task = taskMapper.selectOne(new LambdaQueryWrapper<DriverTask>()
                    .eq(DriverTask::getShiftId, shift.getId()));
            if (task != null && !TaskStatus.RUNNING.equals(task.getStatus())) {
                task.setVehicleId(suggested.getId());
                taskMapper.updateById(task);
            } else if (task != null && TaskStatus.RUNNING.equals(task.getStatus())) {
                throw new BusinessException("司机正在执行该班次，不能调整车辆");
            }
        }
        suggestion.setStatus(SuggestionStatus.CONFIRMED);
        suggestion.setHandleTime(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);
    }

    public void ignore(Long id) {
        DispatchSuggestion suggestion = suggestionMapper.selectById(id);
        if (suggestion == null) {
            throw new BusinessException("调度建议不存在");
        }
        if (!SuggestionStatus.PENDING.equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已处理，请勿重复操作");
        }
        suggestion.setStatus(SuggestionStatus.IGNORED);
        suggestion.setHandleTime(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);
    }

    private int createIfAbsent(Set<String> pendingKeys, ShiftVO shift, BusVehicle current,
                               BusVehicle recommended, String type, String reason) {
        String key = shift.getId() + "|" + type;
        if (pendingKeys.contains(key)) {
            return 0;
        }
        DispatchSuggestion suggestion = new DispatchSuggestion();
        suggestion.setShiftId(shift.getId());
        suggestion.setVehicleId(current == null ? shift.getVehicleId() : current.getId());
        suggestion.setSuggestedVehicleId(recommended == null ? null : recommended.getId());
        suggestion.setType(type);
        suggestion.setReason(reason);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setCreateTime(LocalDateTime.now());
        suggestionMapper.insert(suggestion);
        pendingKeys.add(key);
        return 1;
    }

    /**
     * 选择容量 >= minCapacity 的可用车辆，取容量最小（最合适）的一辆；allowSame 表示是否允许当前车辆。
     */
    private BusVehicle chooseCandidate(List<BusVehicle> candidates, int minCapacity,
                                       boolean allowSame, Long currentVehicleId) {
        return candidates.stream()
                .filter(v -> allowSame || !Objects.equals(v.getId(), currentVehicleId))
                .filter(v -> v.getCapacity() != null && v.getCapacity() >= minCapacity)
                .min(Comparator.comparing(BusVehicle::getCapacity))
                .orElse(null);
    }

    private BusVehicle chooseSmallerCandidate(List<BusVehicle> candidates, int reserved,
                                              int currentCapacity, Long currentVehicleId) {
        return candidates.stream()
                .filter(v -> !Objects.equals(v.getId(), currentVehicleId))
                .filter(v -> v.getCapacity() != null
                        && v.getCapacity() < currentCapacity
                        && v.getCapacity() >= Math.max(reserved, 1))
                .max(Comparator.comparing(BusVehicle::getCapacity))
                .orElse(null);
    }

    private SuggestionVO toVO(DispatchSuggestion suggestion, ShiftVO shift,
                              Map<Long, BusVehicle> vehicleMap) {
        if (shift == null) {
            return null;
        }
        SuggestionVO vo = new SuggestionVO();
        vo.setId(suggestion.getId());
        vo.setShiftId(suggestion.getShiftId());
        vo.setRouteId(shift.getRouteId());
        vo.setRouteName(shift.getRouteName());
        vo.setShiftDate(shift.getShiftDate());
        vo.setDepartTime(shift.getDepartTime());
        vo.setVehicleId(suggestion.getVehicleId());
        BusVehicle vehicle = suggestion.getVehicleId() == null ? null
                : vehicleMap.get(suggestion.getVehicleId());
        vo.setVehiclePlate(vehicle == null ? "未分配" : vehicle.getPlateNo());
        vo.setSuggestedVehicleId(suggestion.getSuggestedVehicleId());
        BusVehicle suggested = suggestion.getSuggestedVehicleId() == null ? null
                : vehicleMap.get(suggestion.getSuggestedVehicleId());
        vo.setSuggestedVehiclePlate(suggested == null ? "" : suggested.getPlateNo());
        vo.setType(suggestion.getType());
        vo.setReason(suggestion.getReason());
        vo.setStatus(suggestion.getStatus());
        vo.setReservedCount(shift.getReservedCount());
        vo.setCapacity(shift.getCapacity());
        vo.setCreateTime(suggestion.getCreateTime());
        vo.setHandleTime(suggestion.getHandleTime());
        return vo;
    }
}
