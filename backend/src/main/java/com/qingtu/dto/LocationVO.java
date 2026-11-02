package com.qingtu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LocationVO {
    private Long shiftId;
    private Long vehicleId;
    private String plateNo;
    private String routeName;
    private String shiftStatus;
    private Double progressPercent;
    private String currentStopName;
    private String nextStopName;
    private Integer etaNextStopMinutes;
    private Integer etaArriveMinutes;
    private String runStatus;
    private Double latitude;
    private Double longitude;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private List<StopNode> stops = new ArrayList<>();

    @Data
    public static class StopNode {
        private Long id;
        private String name;
        private Integer seq;
        /** PASSED / CURRENT / NEXT / WAITING */
        private String state;
    }
}
