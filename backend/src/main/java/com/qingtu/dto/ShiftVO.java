package com.qingtu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShiftVO {
    private Long id;
    private Long routeId;
    private String routeName;
    private Long vehicleId;
    private String vehiclePlate;
    private String vehicleModel;
    private Integer capacity;
    private Long driverId;
    private String driverName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime departTime;
    private String status;
    private Integer reservedCount;
    private Integer remainingSeats;
    private String startStopName;
    private String endStopName;
    private Integer stopCount;
}
