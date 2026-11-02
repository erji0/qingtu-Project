package com.qingtu.common;

public final class SuggestionType {

    /** 预约率 > 95%，建议增加车辆或更换更大容量车辆 */
    public static final String ADD_VEHICLE = "ADD_VEHICLE";
    /** 预约率 80%-95%，建议更换更大容量车辆 */
    public static final String LARGER_VEHICLE = "LARGER_VEHICLE";
    /** 预约率 < 30%，建议调整小容量车辆或合并班次 */
    public static final String REDUCE_CAPACITY = "REDUCE_CAPACITY";
    /** 相邻班次需求差异过大，建议运力重新分配 */
    public static final String REALLOCATE = "REALLOCATE";
    /** 原分配车辆不可用，建议重新分配车辆 */
    public static final String REASSIGN_VEHICLE = "REASSIGN_VEHICLE";

    private SuggestionType() {
    }
}
