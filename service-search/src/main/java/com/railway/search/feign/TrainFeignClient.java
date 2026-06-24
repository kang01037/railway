package com.railway.search.feign;

import com.railway.common.model.R;
import com.railway.search.dto.SeatStockVO;
import com.railway.search.dto.TrainVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调 service-train-stock 获取车次和库存数据。
 */
@FeignClient(name = "service-train-stock", fallbackFactory = TrainFeignClientFallback.class)
public interface TrainFeignClient {

    /** 条件查询车次 */
    @GetMapping("/trains")
    R<List<TrainVO>> queryTrains(@RequestParam(required = false) String startStation,
                                  @RequestParam(required = false) String endStation,
                                  @RequestParam(required = false) Integer status);

    /** 按车次号查询某天的座位库存列表 */
    @GetMapping("/trains/{trainNo}/seats")
    R<List<SeatStockVO>> listSeats(@PathVariable("trainNo") String trainNo,
                                    @RequestParam("runDate") String runDate);

    /** 按车次号查询所有日期的座位库存列表 */
    @GetMapping("/trains/{trainNo}/seats/all")
    R<List<SeatStockVO>> listAllSeats(@PathVariable("trainNo") String trainNo);
}
