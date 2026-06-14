package com.railway.trainstock.mapper;

import com.railway.trainstock.entity.TrainSeatStockDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;


public interface TrainSeatStockMapper {

    int insert(TrainSeatStockDO stock);

    TrainSeatStockDO selectById(@Param("id") Long id);

    /** 按 (train_no, run_date, seat_type) 精确查 */
    TrainSeatStockDO selectByKey(@Param("trainNo") String trainNo,
                                 @Param("runDate") LocalDate runDate,
                                 @Param("seatType") String seatType);

    /** 某车次某天所有座位类型 */
    List<TrainSeatStockDO> listByTrainDate(@Param("trainNo") String trainNo,
                                           @Param("runDate") LocalDate runDate);

    /** 某车次所有库存（不限日期） */
    List<TrainSeatStockDO> listByTrainNo(@Param("trainNo") String trainNo);

    /**
     * 乐观锁扣减：remain &gt;= num 才更新；返回受影响行数（0 = 冲突或库存不足）。
     * 同时把 version +1。
     */
    int decreaseRemainByVersion(@Param("trainNo") String trainNo,
                                @Param("runDate") LocalDate runDate,
                                @Param("seatType") String seatType,
                                @Param("num") int num,
                                @Param("version") int version);

    /**
     * 乐观锁释放：直接 +num。
     */
    int increaseRemainByVersion(@Param("trainNo") String trainNo,
                                @Param("runDate") LocalDate runDate,
                                @Param("seatType") String seatType,
                                @Param("num") int num,
                                @Param("version") int version);
}
