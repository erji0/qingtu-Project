package com.qingtu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qingtu.entity.BusStop;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class TaskVO {
    private Long id;
    private Long shiftId;
    private Long routeId;
    private String routeName;
    private LocalDate shiftDate;
    private LocalTime departTime;
    private Long vehicleId;
    private String plateNo;
    private Integer capacity;
    private Integer reservedCount;
    private Integer remainingSeats;
    private String status;
    private String shiftStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private List<BusStop> stops;
    private LocationVO location;
}
