package com.qingtu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRequest {

    @NotNull(message = "请选择车辆")
    private Long vehicleId;

    @NotNull(message = "请选择司机")
    private Long driverId;
}
