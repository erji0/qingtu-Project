package com.qingtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_suggestion")
public class DispatchSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shiftId;
    private Long vehicleId;
    private Long suggestedVehicleId;
    private String type;
    private String reason;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
}
