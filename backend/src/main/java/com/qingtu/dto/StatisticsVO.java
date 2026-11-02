package com.qingtu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StatisticsVO {
    private Integer totalShifts;
    private Integer totalReservations;
    private Integer totalStudents;
    private Integer totalVehicles;
    private Double avgOccupancyRate;
    private List<RouteStat> routeStats = new ArrayList<>();
    private List<ShiftStat> shiftStats = new ArrayList<>();

    @Data
    public static class RouteStat {
        private Long routeId;
        private String routeName;
        private Integer shiftCount;
        private Integer capacity;
        private Integer reservedCount;
        private Double occupancyRate;
    }

    @Data
    public static class ShiftStat {
        private Long shiftId;
        private Long routeId;
        private String routeName;
        private String departTime;
        private String vehiclePlate;
        private Integer capacity;
        private Integer reservedCount;
        private Double occupancyRate;
        private String status;
    }
}
