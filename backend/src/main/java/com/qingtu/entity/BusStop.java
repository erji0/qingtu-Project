package com.qingtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bus_stop")
public class BusStop {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private String name;
    private Integer seq;
    private Double latitude;
    private Double longitude;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
