package com.qingtu.common;

public final class ShiftStatus {

    public static final String SCHEDULED = "SCHEDULED"; // 已调度
    public static final String RUNNING = "RUNNING"; // 正运行
    public static final String FINISHED = "FINISHED"; // 已完成
    public static final String CANCELLED = "CANCELLED"; // 已取消

    private ShiftStatus() {
    }
}
