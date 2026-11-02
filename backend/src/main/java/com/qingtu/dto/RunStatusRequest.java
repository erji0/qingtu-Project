package com.qingtu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RunStatusRequest {

    @NotBlank(message = "请选择运行状态")
    private String runStatus;
}
