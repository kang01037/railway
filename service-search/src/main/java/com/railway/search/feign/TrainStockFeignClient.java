package com.railway.search.feign;

import com.railway.common.model.R;
import com.railway.search.feign.dto.SeatRemainVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 调 service-train-stock 拿车次座位余票。
 * <p>路径前缀由 service-search 内部约定：
 * <ul>
 *   <li>余票明细：{@code /trains/{trainNo}/seats?runDate=...}</li>
 * </ul>
 */
@FeignClient(name = "service-train-stock", fallbackFactory = TrainStockFeignClientFallback.class)
public interface TrainStockFeignClient {

    @GetMapping("/trains/{trainNo}/seats")
    R<Map<String, SeatRemainVO>> getSeats(@PathVariable("trainNo") String trainNo,
                                          @RequestParam("runDate") String runDate);
}
