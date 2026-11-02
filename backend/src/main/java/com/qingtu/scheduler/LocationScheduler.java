package com.qingtu.scheduler;

import com.qingtu.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationScheduler {

    private final LocationService locationService;

    /**
     * 每 30 秒推进一次运行中车辆的模拟位置。
     */
    @Scheduled(fixedRateString = "${app.simulation.rate:30000}")
    public void advanceVehicleLocation() {
        locationService.advanceAll();
    }
}
