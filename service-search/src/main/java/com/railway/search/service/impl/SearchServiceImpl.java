package com.railway.search.service.impl;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.search.dto.SeatStockVO;
import com.railway.search.dto.TrainVO;
import com.railway.search.feign.TrainFeignClient;
import com.railway.search.feign.dto.SeatRemainVO;
import com.railway.search.index.TrainIndex;
import com.railway.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索服务实现（MySQL + Redis，无 ES 依赖）。
 *
 * <h3>查询策略</h3>
 * <ul>
 *   <li>通过 Feign 调 service-train-stock 查询车次（MySQL）</li>
 *   <li>逐个车次查实时余票（Redis），过滤无票车次</li>
 *   <li>返回车次信息 + 实时余票数量</li>
 * </ul>
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final TrainFeignClient trainFeignClient;

    public SearchServiceImpl(TrainFeignClient trainFeignClient) {
        this.trainFeignClient = trainFeignClient;
    }

    @Override
    public List<TrainIndex> searchTrains(String from, String to, LocalDate date) {
        if (date == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "date 必填");
        }

        // 1. 通过 Feign 查询车次（MySQL），按站点过滤
        R<List<TrainVO>> r = trainFeignClient.queryTrains(from, to, 1);
        if (r == null || !r.isSuccess() || r.getData() == null || r.getData().isEmpty()) {
            log.info("未查询到车次 from={} to={}", from, to);
            return List.of();
        }

        List<TrainVO> trains = r.getData();
        String dateStr = date.toString();
        List<TrainIndex> results = new ArrayList<>();

        // 2. 逐个车次查实时余票（Redis），过滤无票车次
        for (TrainVO train : trains) {
            try {
                R<List<SeatStockVO>> seatR = trainFeignClient.listSeats(train.getTrainNo(), dateStr);
                if (seatR == null || !seatR.isSuccess() || seatR.getData() == null || seatR.getData().isEmpty()) {
                    continue; // 该车次该日期无库存，跳过
                }

                List<SeatStockVO> stocks = seatR.getData();

                // 检查是否有余票（至少一种座位类型 remain > 0）
                boolean hasStock = stocks.stream().anyMatch(s -> s.getRemain() != null && s.getRemain() > 0);
                if (!hasStock) {
                    continue; // 全部售罄，跳过
                }

                // 3. 构建响应（含实时余票数量）
                List<TrainIndex.SeatPrice> prices = stocks.stream()
                        .map(s -> new TrainIndex.SeatPrice(
                                s.getSeatType(),
                                s.getPrice() != null ? s.getPrice().doubleValue() : 0.0,
                                s.getTotal(),
                                s.getRemain()
                        ))
                        .toList();

                TrainIndex doc = TrainIndex.builder()
                        .id(train.getTrainNo())
                        .trainNo(train.getTrainNo())
                        .trainType(train.getTrainType())
                        .startStation(train.getStartStation())
                        .endStation(train.getEndStation())
                        .startTime(train.getStartTime())
                        .endTime(train.getEndTime())
                        .runDays(train.getRunDays())
                        .runDate(date)
                        .prices(prices)
                        .build();

                results.add(doc);
            } catch (Exception e) {
                log.warn("查询车次 {} 余票失败 date={} err={}", train.getTrainNo(), date, e.toString());
            }
        }

        return results;
    }

    @Override
    public List<SeatRemainVO> getSeats(String trainNo, LocalDate date) {
        if (trainNo == null || trainNo.isBlank() || date == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "trainNo / runDate 必填");
        }
        R<List<SeatStockVO>> r = trainFeignClient.listSeats(trainNo, date.toString());
        if (r == null || !r.isSuccess() || r.getData() == null) {
            log.warn("train-stock Feign 拿座位余票失败 trainNo={} date={} msg={}", trainNo, date, r);
            return List.of();
        }
        return r.getData().stream()
                .map(s -> SeatRemainVO.builder()
                        .seatType(s.getSeatType())
                        .price(s.getPrice())
                        .total(s.getTotal())
                        .remain(s.getRemain())
                        .build())
                .toList();
    }
}
