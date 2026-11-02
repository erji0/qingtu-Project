package com.qingtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingtu.entity.BusShift;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BusShiftMapper extends BaseMapper<BusShift> {

    /**
     * 预约时锁定班次行，防止并发超卖。
     */
    @Select("SELECT * FROM bus_shift WHERE id = #{id} FOR UPDATE")
    BusShift selectForUpdate(@Param("id") Long id);
}
