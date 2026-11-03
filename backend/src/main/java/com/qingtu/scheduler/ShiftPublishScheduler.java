package com.qingtu.scheduler;

import com.qingtu.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每天凌晨自动发布班次调度。
 * <p>
 * 规则：以前一天未取消的班次为模板，为"今天"创建新班次（车辆、司机、
 * 发车时间均沿用模板，状态重置为 SCHEDULED），并同步生成司机任务。
 * <p>
 * 幂等设计：同一 (route_id, shift_date, depart_time) 三元组当天已存在
 * 则跳过，重复触发不会产生脏数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftPublishScheduler {

    private final ShiftService shiftService;

    /**
     * 每天 00:05 自动发布今天的班次。
     * cron：秒 分 时 日 月 周
     */
    @Scheduled(cron = "${app.shift.publish.cron:0 5 0 * * ?}")
    public void publishToday() {
        try {
            int created = shiftService.publishDaily(null);
            log.info("[班次自动发布] 今日新发布 {} 条班次", created);
        } catch (Exception e) {
            log.error("[班次自动发布] 执行失败", e);
        }
    }
}
