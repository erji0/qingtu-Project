package com.qingtu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {

    @NotNull(message = "请选择预约班次")
    private Long shiftId;
}
