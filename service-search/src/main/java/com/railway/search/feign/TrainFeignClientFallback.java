package com.railway.search.feign;

import com.railway.common.model.R;
import com.railway.search.dto.SeatStockVO;
import com.railway.search.dto.TrainVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** TrainFeignClient 降级：服务不可用时返空列表。 */
@Slf4j
@Component
public class TrainFeignClientFallback implements FallbackFactory<TrainFeignClient> {

    @Override
    public TrainFeignClient create(Throwable cause) {
        log.warn("TrainFeignClient 降级 cause={}", cause.getMessage());
        return new TrainFeignClient() {
            @Override
            public R<List<TrainVO>> queryTrains(String startStation, String endStation, Integer status) {
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<List<SeatStockVO>> listSeats(String trainNo, String runDate) {
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<List<SeatStockVO>> listAllSeats(String trainNo) {
                return R.ok(Collections.emptyList());
            }
        };
    }
}
