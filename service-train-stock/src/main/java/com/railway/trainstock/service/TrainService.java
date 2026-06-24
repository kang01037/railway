package com.railway.trainstock.service;

import com.railway.trainstock.dto.query.TrainQuery;
import com.railway.trainstock.dto.request.TrainReq;
import com.railway.trainstock.entity.TrainSeatStockDO;
import com.railway.trainstock.vo.SeatStockVO;
import com.railway.trainstock.vo.TrainVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 车次 + 座位库存服务（管理侧）。
 */
public interface TrainService {

    /** 新增车次 + 初始座位库存。{@code @RequireRole("ADMIN")} */
    Long addTrain(TrainReq req);

    /** 修改车次。{@code @RequireRole("ADMIN")} */
    void updateTrain(Long id, TrainReq req);

    /** 删除车次（同时级联删除库存）。{@code @RequireRole("ADMIN")} */
    void deleteTrain(Long id);

    /** 按车次号查询某天的座位库存列表。 */
    List<SeatStockVO> listSeats(String trainNo, LocalDate runDate);

    /** 按车次号查询所有日期的座位库存列表（用于 ES 同步）。 */
    List<SeatStockVO> listAllSeats(String trainNo);

    /** 条件查询车次。 */
    List<TrainVO> queryTrains(TrainQuery query);

    /** 按 ID 查车次。 */
    TrainVO getById(Long id);

    /** 内部用：拿一个 TrainSeatStockDO（含 version）给乐观锁。 */
    TrainSeatStockDO getStockEntity(String trainNo, LocalDate runDate, String seatType);
}
