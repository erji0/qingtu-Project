package com.qingtu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class SuggestionVO {
    private Long id;
    private Long shiftId;
    private Long routeId;
    private String routeName;
    private LocalDate shiftDate;
    private LocalTime departTime;
    private Long vehicleId;
    private String vehiclePlate;
    private Long suggestedVehicleId;
    private String suggestedVehiclePlate;
    private String type;
    private String reason;
    private String status;
    private Integer reservedCount;
    private Integer capacity;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
}
