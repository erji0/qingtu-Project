package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.entity.BusRoute;
import com.qingtu.entity.BusShift;
import com.qingtu.entity.BusStop;
import com.qingtu.mapper.BusRouteMapper;
import com.qingtu.mapper.BusShiftMapper;
import com.qingtu.mapper.BusStopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final BusRouteMapper routeMapper;
    private final BusStopMapper stopMapper;
    private final BusShiftMapper shiftMapper;

    public List<BusRoute> list(boolean onlyEnabled) {
        LambdaQueryWrapper<BusRoute> wrapper = new LambdaQueryWrapper<BusRoute>()
                .orderByAsc(BusRoute::getId);
        if (onlyEnabled) {
            wrapper.eq(BusRoute::getStatus, 1);
        }
        return routeMapper.selectList(wrapper);
    }

    public BusRoute create(BusRoute route) {
        if (route.getName() == null || route.getName().isBlank()) {
            throw new BusinessException("请输入线路名称");
        }
        route.setId(null);
        route.setStatus(route.getStatus() == null ? 1 : route.getStatus());
        route.setCreateTime(LocalDateTime.now());
        routeMapper.insert(route);
        return route;
    }

    public BusRoute update(Long id, BusRoute route) {
        BusRoute exists = routeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("线路不存在");
        }
        exists.setName(route.getName());
        exists.setDescription(route.getDescription());
        if (route.getStatus() != null) {
            exists.setStatus(route.getStatus());
        }
        routeMapper.updateById(exists);
        return exists;
    }

    @Transactional
    public void delete(Long id) {
        BusRoute exists = routeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("线路不存在");
        }
        Long shiftCount = shiftMapper.selectCount(new LambdaQueryWrapper<BusShift>()
                .eq(BusShift::getRouteId, id));
        if (shiftCount != null && shiftCount > 0) {
            throw new BusinessException("该线路已被班次使用，不能删除");
        }
        // 先级联删除该线路下所有站点
        stopMapper.delete(new LambdaQueryWrapper<BusStop>()
                .eq(BusStop::getRouteId, id));
        routeMapper.deleteById(id);
    }
}
