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

/**
 * 根据条件获取公交线路列表
 * @param onlyEnabled 是否只获取启用的线路，true表示只获取状态为1的线路，false表示获取所有线路
 * @return 返回符合条件的公交线路列表，按ID升序排列
 */
    public List<BusRoute> list(boolean onlyEnabled) {
    // 创建Lambda查询条件构造器，并按ID升序排序
        LambdaQueryWrapper<BusRoute> wrapper = new LambdaQueryWrapper<BusRoute>()
                .orderByAsc(BusRoute::getId);
    // 如果只查询启用的线路，添加状态等于1的条件
        if (onlyEnabled) {
            wrapper.eq(BusRoute::getStatus, 1);
        }
    // 执行查询并返回结果列表
        return routeMapper.selectList(wrapper);
    }

    /**
     * 创建新的公交线路
     * @param route 包含线路信息的BusRoute对象
     * @return 创建后的BusRoute对象，包含生成的ID和其他信息
     * @throws BusinessException 当线路名称为空或空白时抛出异常
     */
    public BusRoute create(BusRoute route) {
    // 检查线路名称是否为空或空白，如果是则抛出业务异常
        if (route.getName() == null || route.getName().isBlank()) {
            throw new BusinessException("请输入线路名称");
        }
    // 设置线路ID为null，让数据库自动生成
        route.setId(null);
    // 设置线路状态，如果未指定状态则默认为1
        route.setStatus(route.getStatus() == null ? 1 : route.getStatus());
    // 设置创建时间为当前时间
        route.setCreateTime(LocalDateTime.now());
    // 将线路信息插入数据库
        routeMapper.insert(route);
    // 返回创建后的线路对象
        return route;
    }

/**
 * 更新公交线路信息
 * @param id 要更新的公交线路ID
 * @param route 包含更新后信息的公交线路对象
 * @return 更新后的完整公交线路信息
 * @throws BusinessException 当指定的线路ID不存在时抛出
 */
    public BusRoute update(Long id, BusRoute route) {
    // 根据ID查询现有的公交线路信息
        BusRoute exists = routeMapper.selectById(id);
    // 如果线路不存在，抛出业务异常
        if (exists == null) {
            throw new BusinessException("线路不存在");
        }
    // 更新线路名称
        exists.setName(route.getName());
    // 更新线路描述
        exists.setDescription(route.getDescription());
    // 如果线路状态不为空，则更新线路状态
        if (route.getStatus() != null) {
            exists.setStatus(route.getStatus());
        }
    // 执行更新操作
        routeMapper.updateById(exists);
    // 返回更新后的线路信息
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
