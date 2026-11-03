package com.qingtu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实时公交展示模型：线路 + 站点列表 + 每个站点附近活跃车辆。
 */
@Data
public class LiveRouteVO {

    private RouteInfo route;
    private List<StopInfo> stops = new ArrayList<>();

    @Data
    public static class RouteInfo {
        private Long id;
        private String name;
        /** 当前运行中车辆数 */
        private Integer activeCount;
    }

    @Data
    public static class StopInfo {
        private Long id;
        private String name;
        private Integer seq;
        private Double latitude;
        private Double longitude;
        /** 到达该站的活跃车辆，按 ETA 升序 */
        private List<VehicleArrival> vehicles = new ArrayList<>();
    }

    @Data
    public static class VehicleArrival {
        private Long shiftId;
        private String plateNo;
        private String currentStopName;
        /** 到本站点需多少分钟，0 表示已过 */
        private Integer etaMinutes;
        /** 班次出发时间 HH:mm */
        private String departTime;
    }
}
