package com.qingtu.dto;

import com.qingtu.entity.BusStop;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftDetailVO extends ShiftVO {
    private List<BusStop> stops;
}
