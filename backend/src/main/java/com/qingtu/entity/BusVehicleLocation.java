package com.qingtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bus_vehicle_location")
public class BusVehicleLocation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vehicleId;
    private Long shiftId;
    private Long currentStopId;
    private Long nextStopId;
    private Double progress;
    private String runStatus;
    private Double latitude;
    private Double longitude;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
