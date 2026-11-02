package com.qingtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("bus_shift")
public class BusShift {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime departTime;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
