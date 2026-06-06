package com.railway.trainstock.mapper;

import com.railway.trainstock.entity.TrainDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车次表 Mapper。
 */
@Mapper
public interface TrainMapper {

    int insert(TrainDO train);

    TrainDO selectById(@Param("id") Long id);

    TrainDO selectByTrainNo(@Param("trainNo") String trainNo);

    /** 条件查询（任意入参可空）。条件：始发站 + 终点站 + 状态=1。 */
    List<TrainDO> listByCondition(@Param("startStation") String startStation,
                                  @Param("endStation") String endStation,
                                  @Param("status") Integer status);

    /** 某车次在指定日期是否运行（按 run_days 0/1 字符串） */
    int countRunningOnDate(@Param("trainNo") String trainNo,
                           @Param("runDate") String runDate);

    int updateById(TrainDO train);

    int deleteById(@Param("id") Long id);
}
