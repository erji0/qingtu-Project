package com.qingtu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationVO {
    private Long id;
    private Long shiftId;
    private String studentName;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
    private Long routeId;
    private String routeName;
    private LocalDate shiftDate;
    private LocalTime departTime;
    private String vehiclePlate;
    private Integer capacity;
    private Integer reservedCount;
    private Integer remainingSeats;
    private String shiftStatus;
}
