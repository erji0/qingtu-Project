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

    /**
     * 生成调度建议的方法，根据班次的预约情况生成相应的车辆调度建议
     * @param date 指定日期，如果为null则使用当前日期
     * @return 返回创建的调度建议数量
     */
    @Transactional
    public int generate(LocalDate date) {
    // 如果未指定日期，则使用当前日期
        LocalDate target = date == null ? LocalDate.now() : date;
    // 获取指定日期的所有未取消和未完成的班次
        List<ShiftVO> shifts = shiftService.list(target, null).stream()
                .filter(s -> !ShiftStatus.CANCELLED.equals(s.getStatus())
                        && !ShiftStatus.FINISHED.equals(s.getStatus()))
                .collect(Collectors.toList());
    // 如果没有符合条件的班次，直接返回0
        if (shifts.isEmpty()) {
            return 0;
        }
    // 获取所有可用的车辆
        List<BusVehicle> candidates = vehicleMapper.selectList(new LambdaQueryWrapper<BusVehicle>()
                .eq(BusVehicle::getStatus, VehicleStatus.AVAILABLE));
    // 获取所有待处理的调度建议的键（班次ID+类型）
        Set<String> pendingKeys = suggestionMapper.selectList(new LambdaQueryWrapper<DispatchSuggestion>()
                        .eq(DispatchSuggestion::getStatus, SuggestionStatus.PENDING))
                .stream()
                .map(s -> s.getShiftId() + "|" + s.getType())
                .collect(Collectors.toSet());

    // 初始化创建计数器、预约率映射和班次映射
        int created = 0;
        Map<Long, Double> rateMap = new HashMap<>();
        Map<Long, ShiftVO> shiftMap = shifts.stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));

    // 遍历每个班次，根据预约率生成调度建议
        for (ShiftVO shift : shifts) {
        // 计算班次的预约率和容量
            int reserved = shift.getReservedCount() == null ? 0 : shift.getReservedCount();
            int capacity = shift.getCapacity() == null ? 0 : shift.getCapacity();
            double rate = capacity == 0 ? 0 : reserved * 1.0 / capacity;
            rateMap.put(shift.getId(), rate);

        // 检查当前分配的车辆是否有效
            BusVehicle current = shift.getVehicleId() == null ? null
                    : vehicleMapper.selectById(shift.getVehicleId());
            if (current == null || VehicleStatus.DISABLED.equals(current.getStatus())) {
            // 如果没有分配车辆或车辆已停用，建议重新分配
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, 1), false, null);
                String reason = current == null
                        ? "班次尚未分配车辆，建议重新分配可用车辆"
                        : "原车辆 " + current.getPlateNo() + " 已停用，建议重新分配可用车辆";
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.REASSIGN_VEHICLE, reason);
                continue;
            }

        // 根据不同的预约率阈值生成不同的调度建议
            if (rate > 0.95) {
            // 预约率超过95%，建议增加车辆或更换更大容量车辆
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, capacity + 1), false, current.getId());
                String reason = String.format("预约率 %.1f%% 已超过 95%%，建议增加车辆或更换更大容量车辆", rate * 100);
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.ADD_VEHICLE, reason);
            } else if (rate >= 0.80) {
            // 预约率在80%-95%之间，建议更换更大容量车辆
                BusVehicle recommended = chooseCandidate(candidates, Math.max(reserved, capacity + 1), false, current.getId());
                String reason = String.format("预约率 %.1f%% 处于 80%%-95%%，建议更换更大容量车辆", rate * 100);
                created += createIfAbsent(pendingKeys, shift, current, recommended,
                        SuggestionType.LARGER_VEHICLE, reason);
            } else if (rate < 0.30) {
            // 预约率低于30%，建议调整车辆（小容量）或合并班次
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

    /**
     * 根据状态和日期获取调度建议列表
     * @param status 调度建议状态，可为null
     * @param date 查询日期，可为null
     * @return 调度建议列表，如果无数据则返回空列表
     */
    public List<SuggestionVO> list(String status, LocalDate date) {
    // 创建查询条件，按创建时间降序排列
        LambdaQueryWrapper<DispatchSuggestion> wrapper = new LambdaQueryWrapper<DispatchSuggestion>()
                .orderByDesc(DispatchSuggestion::getCreateTime);
    // 如果状态不为空，添加状态查询条件
        if (status != null && !status.isBlank()) {
            wrapper.eq(DispatchSuggestion::getStatus, status);
        }
    // 根据查询条件获取调度建议列表
        List<DispatchSuggestion> suggestions = suggestionMapper.selectList(wrapper);
    // 如果没有数据，返回空列表
        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }
    // 获取所有班次ID集合
        Set<Long> shiftIds = suggestions.stream().map(DispatchSuggestion::getShiftId)
                .collect(Collectors.toSet());
    // 批量获取班次信息
        List<BusShift> shifts = shiftMapper.selectBatchIds(shiftIds);
    // 如果指定了日期，则筛选出指定日期的班次
        if (date != null) {
        // 获取指定日期的班次ID列表
            List<Long> dateShiftIds = shifts.stream()
                    .filter(s -> date.equals(s.getShiftDate()))
                    .map(BusShift::getId).collect(Collectors.toList());
        // 筛选出属于指定日期的调度建议
            suggestions = suggestions.stream()
                    .filter(s -> dateShiftIds.contains(s.getShiftId()))
                    .collect(Collectors.toList());
        // 如果筛选后没有数据，返回空列表
            if (suggestions.isEmpty()) {
                return Collections.emptyList();
            }
        }
    // 构建班次VO映射表
        Map<Long, ShiftVO> shiftMap = shiftService.buildVOs(shifts).stream()
                .collect(Collectors.toMap(ShiftVO::getId, Function.identity()));
    // 收集所有涉及的车辆ID
        Set<Long> vehicleIds = new HashSet<>();
        suggestions.forEach(s -> {
            if (s.getVehicleId() != null) {
                vehicleIds.add(s.getVehicleId());
            }
            if (s.getSuggestedVehicleId() != null) {
                vehicleIds.add(s.getSuggestedVehicleId());
            }
        });
    // 批量获取车辆信息并构建映射表
        Map<Long, BusVehicle> vehicleMap = vehicleIds.isEmpty() ? Collections.emptyMap()
                : vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(BusVehicle::getId, Function.identity()));

    // 将调度建议转换为VO对象并返回结果列表
        return suggestions.stream()
                .map(s -> toVO(s, shiftMap.get(s.getShiftId()), vehicleMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 确认调度建议的方法
     * 该方法用于处理调度建议的确认操作，包括验证建议有效性、更换车辆等逻辑
     * @param id 调度建议ID
     * @throws BusinessException 当建议不存在、建议已处理、关联班次不存在、推荐车辆不存在或停用、车辆容量不足等情况时抛出
     */
    @Transactional
    public void confirm(Long id) {
    // 根据ID查询调度建议
        DispatchSuggestion suggestion = suggestionMapper.selectById(id);
    // 检查调度建议是否存在
        if (suggestion == null) {
            throw new BusinessException("调度建议不存在");
        }
    // 检查建议状态是否为待处理，防止重复操作
        if (!SuggestionStatus.PENDING.equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已处理，请勿重复操作");
        }
    // 根据建议中的班次ID查询班次信息
        BusShift shift = shiftMapper.selectById(suggestion.getShiftId());
    // 检查关联班次是否存在
        if (shift == null) {
            throw new BusinessException("关联班次不存在");
        }
    // 如果建议中有推荐车辆ID
        if (suggestion.getSuggestedVehicleId() != null) {
        // 查询推荐车辆信息
            BusVehicle suggested = vehicleMapper.selectById(suggestion.getSuggestedVehicleId());
        // 检查推荐车辆是否存在
            if (suggested == null) {
                throw new BusinessException("推荐车辆不存在");
            }
        // 检查推荐车辆是否已停用
            if (VehicleStatus.DISABLED.equals(suggested.getStatus())) {
                throw new BusinessException("推荐车辆已停用，请改用其他车辆");
            }
        // 获取该班次的已预约人数
            int reserved = shiftService.activeReservedCount(shift.getId());
        // 检查推荐车辆容量是否足够
            if (suggested.getCapacity() < reserved) {
                throw new BusinessException("推荐车辆容量不足以容纳已有预约人数");
            }
        // 保存原车辆ID
            Long oldVehicleId = shift.getVehicleId();
        // 更新班次车辆ID为推荐车辆
            shift.setVehicleId(suggested.getId());
            shiftMapper.updateById(shift);
        // 如果原车辆存在且与新车辆不同
            if (oldVehicleId != null && !oldVehicleId.equals(suggested.getId())) {
            // 查询原车辆信息
                BusVehicle old = vehicleMapper.selectById(oldVehicleId);
            // 如果原车辆存在且未停用，将其状态设为可用
                if (old != null && !VehicleStatus.DISABLED.equals(old.getStatus())) {
                    old.setStatus(VehicleStatus.AVAILABLE);
                    vehicleMapper.updateById(old);
                }
            }
        // 查询与班次关联的司机任务
            DriverTask task = taskMapper.selectOne(new LambdaQueryWrapper<DriverTask>()
                    .eq(DriverTask::getShiftId, shift.getId()));
        // 如果任务存在且未运行中，更新任务中的车辆ID
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

    /**
     * 忽略调度建议的方法
     * @param id 调度建议的唯一标识符
     * @throws BusinessException 当建议不存在或已处理时抛出业务异常
     */
    public void ignore(Long id) {
    // 根据ID查询调度建议
        DispatchSuggestion suggestion = suggestionMapper.selectById(id);
    // 如果建议不存在，抛出业务异常
        if (suggestion == null) {
            throw new BusinessException("调度建议不存在");
        }
    // 检查建议状态是否为待处理，如果不是则抛出异常
        if (!SuggestionStatus.PENDING.equals(suggestion.getStatus())) {
            throw new BusinessException("该建议已处理，请勿重复操作");
        }
    // 设置建议状态为已忽略
        suggestion.setStatus(SuggestionStatus.IGNORED);
    // 设置处理时间为当前时间
        suggestion.setHandleTime(LocalDateTime.now());
    // 更新数据库中的建议信息
        suggestionMapper.updateById(suggestion);
    }

    /**
     * 创建一个调度建议，如果该建议不存在于待处理集合中
     *
     * @param pendingKeys 已存在的调度建议键集合，用于避免重复创建
     * @param shift 班次信息对象
     * @param current 当前分配的车辆信息
     * @param recommended 建议分配的车辆信息
     * @param type 调度建议类型
     * @param reason 调度建议的原因
     * @return 返回1表示成功创建新建议，返回0表示建议已存在
     */
    private int createIfAbsent(Set<String> pendingKeys, ShiftVO shift, BusVehicle current,
                               BusVehicle recommended, String type, String reason) {
    // 生成唯一键，由班次ID和建议类型组成
        String key = shift.getId() + "|" + type;
    // 检查该键是否已存在于待处理集合中
        if (pendingKeys.contains(key)) {
            return 0;  // 如果已存在，直接返回0
        }
    // 创建新的调度建议对象
        DispatchSuggestion suggestion = new DispatchSuggestion();
    // 设置班次ID
        suggestion.setShiftId(shift.getId());
    // 设置车辆ID（如果当前车辆为空，则使用班次原分配的车辆）
        suggestion.setVehicleId(current == null ? shift.getVehicleId() : current.getId());
    // 设置建议的车辆ID（如果推荐车辆为空，则设为null）
        suggestion.setSuggestedVehicleId(recommended == null ? null : recommended.getId());
    // 设置建议类型
        suggestion.setType(type);
    // 设置建议原因
        suggestion.setReason(reason);
    // 设置建议状态为待处理
        suggestion.setStatus(SuggestionStatus.PENDING);
    // 设置创建时间为当前时间
        suggestion.setCreateTime(LocalDateTime.now());
    // 将新建议插入数据库
        suggestionMapper.insert(suggestion);
    // 将键添加到待处理集合中，避免重复创建
        pendingKeys.add(key);
        return 1;  // 返回1表示成功创建
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

    /**
     * 从候选车辆中选择一个容量较小但仍能满足需求的车辆
     *
     * @param candidates 候选车辆列表
     * @param reserved 需要预留的座位数
     * @param currentCapacity 当前车辆的容量
     * @param currentVehicleId 当前车辆的ID
     * @return 符合条件的容量较小的车辆，如果没有合适的则返回null
     */
    private BusVehicle chooseSmallerCandidate(List<BusVehicle> candidates, int reserved,
                                              int currentCapacity, Long currentVehicleId) {
    // 使用Stream流处理候选车辆列表
        return candidates.stream()
            // 过滤掉当前正在使用的车辆
                .filter(v -> !Objects.equals(v.getId(), currentVehicleId))
            // 过选出容量小于当前车辆容量且不小于预留座位数的车辆
                .filter(v -> v.getCapacity() != null
                        && v.getCapacity() < currentCapacity
                        && v.getCapacity() >= Math.max(reserved, 1))
            // 从符合条件的车辆中选择容量最大的一个（即相对较小的容量）
                .max(Comparator.comparing(BusVehicle::getCapacity))
            // 如果没有符合条件的车辆，则返回null
                .orElse(null);
    }

    /**
     * 将调度建议实体转换为视图对象(VO)
     * @param suggestion 调度建议实体
     * @param shift 班次视图对象
     * @param vehicleMap 车辆ID与车辆实体的映射关系
     * @return 转换后的建议视图对象，如果班次为空则返回null
     */
    private SuggestionVO toVO(DispatchSuggestion suggestion, ShiftVO shift,
                              Map<Long, BusVehicle> vehicleMap) {
    // 如果班次为空，直接返回null
        if (shift == null) {
            return null;
        }
    // 创建新的建议视图对象
        SuggestionVO vo = new SuggestionVO();
    // 设置建议基本信息
        vo.setId(suggestion.getId());
        vo.setShiftId(suggestion.getShiftId());
    // 设置班次相关信息
        vo.setRouteId(shift.getRouteId());
        vo.setRouteName(shift.getRouteName());
        vo.setShiftDate(shift.getShiftDate());
        vo.setDepartTime(shift.getDepartTime());
    // 设置当前分配车辆信息
        vo.setVehicleId(suggestion.getVehicleId());
    // 根据车辆ID从映射中获取车辆实体，如果未分配则显示"未分配"
        BusVehicle vehicle = suggestion.getVehicleId() == null ? null
                : vehicleMap.get(suggestion.getVehicleId());
        vo.setVehiclePlate(vehicle == null ? "未分配" : vehicle.getPlateNo());
    // 设置建议车辆信息
        vo.setSuggestedVehicleId(suggestion.getSuggestedVehicleId());
    // 根据建议车辆ID从映射中获取车辆实体，如果未分配则为空字符串
        BusVehicle suggested = suggestion.getSuggestedVehicleId() == null ? null
                : vehicleMap.get(suggestion.getSuggestedVehicleId());
        vo.setSuggestedVehiclePlate(suggested == null ? "" : suggested.getPlateNo());
    // 设置建议类型、原因和状态
        vo.setType(suggestion.getType());
        vo.setReason(suggestion.getReason());
        vo.setStatus(suggestion.getStatus());
    // 设置班次容量信息
        vo.setReservedCount(shift.getReservedCount());
        vo.setCapacity(shift.getCapacity());
    // 设置时间信息
        vo.setCreateTime(suggestion.getCreateTime());
        vo.setHandleTime(suggestion.getHandleTime());
        return vo;
    }
}
