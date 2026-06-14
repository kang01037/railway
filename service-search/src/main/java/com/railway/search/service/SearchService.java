package com.railway.search.service;

import com.railway.search.feign.dto.SeatRemainVO;
import com.railway.search.index.TrainIndex;

import java.time.LocalDate;
import java.util.List;

/**
 * 检索服务：车次 / 座位余票。
 */
public interface SearchService {

    /**
     * 按 出发站 / 到达站 / 日期 检索车次。
     * <p>ES 查询：startStation / endStation text 匹配，runDate 精确。
     */
    List<TrainIndex> searchTrains(String from, String to, LocalDate date);

    /**
     * 查某车次某天的座位余票明细。Feign → service-train-stock。
     */
    List<SeatRemainVO> getSeats(String trainNo, LocalDate date);
}
