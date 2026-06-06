package com.railway.search.feign;

import com.railway.common.model.R;
import com.railway.search.feign.dto.SeatRemainVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Feign 降级：服务不可用时返 fail，由 SearchController 翻译。 */
@Slf4j
@Component
public class TrainStockFeignClientFallback implements FallbackFactory<TrainStockFeignClient> {

    @Override
    public TrainStockFeignClient create(Throwable cause) {
        log.warn("TrainStockFeignClient 降级 cause={}", cause.getMessage());
        return (trainNo, runDate) -> R.fail(503, "库存服务暂不可用");
    }
}
