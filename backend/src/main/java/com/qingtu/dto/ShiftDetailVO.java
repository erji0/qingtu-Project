package com.qingtu.dto;

import com.qingtu.entity.BusStop;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShiftDetailVO extends ShiftVO {
    private List<BusStop> stops;
    /** 当前登录学生在该班次的有效预约 ID，无预约则为 null */
    private Long myReservationId;
    /** 当前登录学生在该班次的预约状态：RESERVED / SIGNED */
    private String myReservationStatus;
}
