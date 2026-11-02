package com.qingtu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.common.BusinessException;
import com.qingtu.entity.BusRoute;
import com.qingtu.entity.BusStop;
import com.qingtu.mapper.BusRouteMapper;
import com.qingtu.mapper.BusStopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StopService {

    private final BusStopMapper stopMapper;
    private final BusRouteMapper routeMapper;

    public List<BusStop> list(Long routeId) {
        LambdaQueryWrapper<BusStop> wrapper = new LambdaQueryWrapper<BusStop>()
                .orderByAsc(BusStop::getRouteId)
                .orderByAsc(BusStop::getSeq);
        if (routeId != null) {
            wrapper.eq(BusStop::getRouteId, routeId);
        }
        return stopMapper.selectList(wrapper);
    }

    public List<BusStop> listByRoute(Long routeId) {
        return stopMapper.selectList(new LambdaQueryWrapper<BusStop>()
                .eq(BusStop::getRouteId, routeId)
                .orderByAsc(BusStop::getSeq));
    }

    public BusStop create(BusStop stop) {
        validate(stop);
        stop.setId(null);
        stop.setCreateTime(LocalDateTime.now());
        try {
            stopMapper.insert(stop);
        } catch (Exception e) {
            throw new BusinessException("同一线路下站点顺序不能重复");
        }
        return stop;
    }

    public BusStop update(Long id, BusStop stop) {
        BusStop exists = stopMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("站点不存在");
        }
        validate(stop);
        exists.setRouteId(stop.getRouteId());
        exists.setName(stop.getName());
        exists.setSeq(stop.getSeq());
        exists.setLatitude(stop.getLatitude());
        exists.setLongitude(stop.getLongitude());
        try {
            stopMapper.updateById(exists);
        } catch (Exception e) {
            throw new BusinessException("同一线路下站点顺序不能重复");
        }
        return exists;
    }

    public void delete(Long id) {
        if (stopMapper.selectById(id) == null) {
            throw new BusinessException("站点不存在");
        }
        stopMapper.deleteById(id);
    }

    private void validate(BusStop stop) {
        if (stop.getRouteId() == null) {
            throw new BusinessException("请选择所属线路");
        }
        BusRoute route = routeMapper.selectById(stop.getRouteId());
        if (route == null) {
            throw new BusinessException("所属线路不存在");
        }
        if (stop.getName() == null || stop.getName().isBlank()) {
            throw new BusinessException("请输入站点名称");
        }
        if (stop.getSeq() == null || stop.getSeq() < 1) {
            throw new BusinessException("站点顺序必须大于 0");
        }
    }
}
