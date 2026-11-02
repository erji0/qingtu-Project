package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.common.RunStatus;
import com.qingtu.common.ShiftStatus;
import com.qingtu.dto.LocationVO;
import com.qingtu.entity.BusRoute;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusStop;
import com.qingtu.entity.BusVehicle;
import com.qingtu.entity.BusVehicleLocation;
import com.qingtu.mapper.BusRouteMapper;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusStopMapper;
import com.qingtu.mapper.BusVehicleLocationMapper;
import com.qingtu.mapper.BusVehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final BusVehicleLocationMapper locationMapper;
    private final BusShiftMapper shiftMapper;
    private final BusStopMapper stopMapper;
    private final BusVehicleMapper vehicleMapper;
    private final BusRouteMapper routeMapper;

    @Value("${app.simulation.tick-seconds:30}")
    private int tickSeconds;

    @Value("${app.simulation.seconds-per-segment:45}")
    private int secondsPerSegment;

    public BusVehicleLocation findByShift(Long shiftId) {
        return locationMapper.selectOne(new LambdaQueryWrapper<BusVehicleLocation>()
                .eq(BusVehicleLocation::getShiftId, shiftId));
    }

    public void initLocation(BusShift shift) {
        if (findByShift(shift.getId()) != null) {
            return;
        }
        List<BusStop> stops = listStops(shift.getRouteId());
        if (stops.isEmpty()) {
            return;
        }
        BusVehicleLocation location = new BusVehicleLocation();
        location.setShiftId(shift.getId());
        location.setVehicleId(shift.getVehicleId());
        location.setProgress(0.0);
        location.setRunStatus(RunStatus.RUNNING);
        location.setCurrentStopId(stops.get(0).getId());
        location.setNextStopId(stops.size() > 1 ? stops.get(1).getId() : null);
        location.setLatitude(stops.get(0).getLatitude());
        location.setLongitude(stops.get(0).getLongitude());
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.insert(location);
    }

    /**
     * 定时任务调用：按时间推进所有运行中班次的模拟位置。
     */
    public void advanceAll() {
        List<BusShift> runningShifts = shiftMapper.selectList(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getStatus, ShiftStatus.RUNNING));
        for (BusShift shift : runningShifts) {
            try {
                BusVehicleLocation location = findByShift(shift.getId());
                if (location == null) {
                    initLocation(shift);
                    location = findByShift(shift.getId());
                }
                if (location == null) {
                    continue;
                }
                List<BusStop> stops = listStops(shift.getRouteId());
                if (stops.size() < 2 || RunStatus.ARRIVED.equals(location.getRunStatus())) {
                    continue;
                }
                double maxProgress = stops.size() - 1;
                double progress = location.getProgress() == null ? 0 : location.getProgress();
                LocalDateTime updateTime = location.getUpdateTime();
                double elapsed = updateTime == null ? tickSeconds
                        : Math.max(tickSeconds, Duration.between(updateTime, LocalDateTime.now()).getSeconds());
                // 防止系统休眠后一次性跳到终点
                elapsed = Math.min(elapsed, tickSeconds * 2.0);
                double newProgress = Math.min(maxProgress, progress + elapsed / secondsPerSegment);
                location.setProgress(newProgress);
                location.setVehicleId(shift.getVehicleId());

                if (newProgress >= maxProgress) {
                    BusStop last = stops.get(stops.size() - 1);
                    location.setCurrentStopId(last.getId());
                    location.setNextStopId(null);
                    location.setLatitude(last.getLatitude());
                    location.setLongitude(last.getLongitude());
                    location.setRunStatus(RunStatus.ARRIVED);
                } else {
                    int currentIndex = (int) Math.floor(newProgress);
                    int nextIndex = currentIndex + 1;
                    double fraction = newProgress - currentIndex;
                    BusStop current = stops.get(currentIndex);
                    BusStop next = stops.get(nextIndex);
                    location.setCurrentStopId(current.getId());
                    location.setNextStopId(next.getId());
                    location.setLatitude(interpolate(current.getLatitude(), next.getLatitude(), fraction));
                    location.setLongitude(interpolate(current.getLongitude(), next.getLongitude(), fraction));
                }
                location.setUpdateTime(LocalDateTime.now());
                locationMapper.updateById(location);
            } catch (Exception e) {
                // 单个班次推进失败不影响其他班次
                e.printStackTrace();
            }
        }
    }

    public LocationVO getByShift(Long shiftId) {
        BusShift shift = shiftMapper.selectById(shiftId);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        BusVehicleLocation location = findByShift(shiftId);
        if (location == null && ShiftStatus.RUNNING.equals(shift.getStatus())) {
            initLocation(shift);
            location = findByShift(shiftId);
        }
        if (location == null) {
            return null;
        }
        return buildVO(shift, location);
    }

    public List<LocationVO> listActive() {
        List<BusShift> shifts = shiftMapper.selectList(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getStatus, ShiftStatus.RUNNING));
        if (shifts.isEmpty()) {
            return new ArrayList<>();
        }
        return shifts.stream()
                .map(shift -> getByShift(shift.getId()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(LocationVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public LocationVO updateRunStatus(Long shiftId, String runStatus) {
        if (!RunStatus.RUNNING.equals(runStatus) && !RunStatus.DELAYED.equals(runStatus)) {
            throw new BusinessException("运行状态只能设置为正常运行或延误");
        }
        BusShift shift = shiftMapper.selectById(shiftId);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        BusVehicleLocation location = findByShift(shiftId);
        if (location == null) {
            initLocation(shift);
            location = findByShift(shiftId);
        }
        if (location == null) {
            throw new BusinessException("车辆尚未开始运行");
        }
        location.setRunStatus(runStatus);
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.updateById(location);
        return buildVO(shift, location);
    }

    public void finishLocation(Long shiftId) {
        BusVehicleLocation location = findByShift(shiftId);
        if (location == null) {
            BusShift shift = shiftMapper.selectById(shiftId);
            if (shift == null) {
                return;
            }
            initLocation(shift);
            location = findByShift(shiftId);
        }
        if (location == null) {
            return;
        }
        List<BusStop> stops = listStops(shiftMapper.selectById(shiftId).getRouteId());
        if (stops.isEmpty()) {
            return;
        }
        BusStop last = stops.get(stops.size() - 1);
        location.setProgress((double) (stops.size() - 1));
        location.setCurrentStopId(last.getId());
        location.setNextStopId(null);
        location.setLatitude(last.getLatitude());
        location.setLongitude(last.getLongitude());
        location.setRunStatus(RunStatus.ARRIVED);
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.updateById(location);
    }

    private LocationVO buildVO(BusShift shift, BusVehicleLocation location) {
        List<BusStop> stops = listStops(shift.getRouteId());
        LocationVO vo = new LocationVO();
        vo.setShiftId(shift.getId());
        vo.setShiftStatus(shift.getStatus());
        vo.setVehicleId(shift.getVehicleId());
        BusVehicle vehicle = shift.getVehicleId() == null ? null : vehicleMapper.selectById(shift.getVehicleId());
        vo.setPlateNo(vehicle == null ? "未分配" : vehicle.getPlateNo());
        BusRoute route = routeMapper.selectById(shift.getRouteId());
        vo.setRouteName(route == null ? "未知线路" : route.getName());
        vo.setRunStatus(location.getRunStatus());
        vo.setLatitude(location.getLatitude());
        vo.setLongitude(location.getLongitude());
        vo.setUpdateTime(location.getUpdateTime());

        if (stops.isEmpty()) {
            vo.setProgressPercent(0.0);
            return vo;
        }
        double maxProgress = stops.size() - 1;
        double progress = location.getProgress() == null ? 0 : location.getProgress();
        progress = Math.max(0, Math.min(maxProgress, progress));
        vo.setProgressPercent(maxProgress == 0 ? 100.0 : round1(progress / maxProgress * 100));

        int currentIndex = (int) Math.floor(progress);
        if (RunStatus.ARRIVED.equals(location.getRunStatus())) {
            currentIndex = (int) maxProgress;
        }
        currentIndex = Math.max(0, Math.min(stops.size() - 1, currentIndex));
        int nextIndex = Math.min(stops.size() - 1, currentIndex + 1);
        BusStop current = stops.get(currentIndex);
        vo.setCurrentStopName(current.getName());
        boolean arrived = RunStatus.ARRIVED.equals(location.getRunStatus()) || currentIndex == stops.size() - 1;
        vo.setNextStopName(arrived ? "已到达终点" : stops.get(nextIndex).getName());

        double fraction = progress - Math.floor(progress);
        int etaNext = arrived ? 0 : (int) Math.ceil((1 - fraction) * secondsPerSegment / 60.0);
        int etaArrive = arrived ? 0 : (int) Math.ceil((maxProgress - progress) * secondsPerSegment / 60.0);
        vo.setEtaNextStopMinutes(Math.max(etaNext, arrived ? 0 : 1));
        vo.setEtaArriveMinutes(Math.max(etaArrive, arrived ? 0 : 1));

        for (int i = 0; i < stops.size(); i++) {
            BusStop stop = stops.get(i);
            LocationVO.StopNode node = new LocationVO.StopNode();
            node.setId(stop.getId());
            node.setName(stop.getName());
            node.setSeq(stop.getSeq());
            if (i < currentIndex) {
                node.setState("PASSED");
            } else if (i == currentIndex) {
                node.setState("CURRENT");
            } else if (i == currentIndex + 1 && !arrived) {
                node.setState("NEXT");
            } else {
                node.setState("WAITING");
            }
            vo.getStops().add(node);
        }
        return vo;
    }

    private List<BusStop> listStops(Long routeId) {
        return stopMapper.selectList(new LambdaQueryWrapper<BusStop>()
                .eq(BusStop::getRouteId, routeId)
                .orderByAsc(BusStop::getSeq));
    }

    private Double interpolate(Double from, Double to, double fraction) {
        if (from == null || to == null) {
            return from == null ? to : from;
        }
        return from + (to - from) * fraction;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
