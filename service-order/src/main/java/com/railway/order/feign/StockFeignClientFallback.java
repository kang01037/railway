package com.railway.order.feign;

import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.order.feign.dto.ConfirmStockDTO;
import com.railway.order.feign.dto.OccupyStockDTO;
import com.railway.order.feign.dto.ReleaseStockDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 库存服务降级：所有调用失败 → R.fail(SERVER_ERROR)。
 * 防止 stock 宕机时整个下单链雪崩。
 */
@Slf4j
@Component
public class StockFeignClientFallback implements FallbackFactory<StockFeignClient> {

    @Override
    public StockFeignClient create(Throwable cause) {
        log.error("StockFeignClient 降级 cause={}", cause.getMessage(), cause);
        return new StockFeignClient() {
            @Override
            public R<Void> occupy(OccupyStockDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "库存服务暂不可用");
            }

            @Override
            public R<Void> confirm(ConfirmStockDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "库存服务暂不可用");
            }

            @Override
            public R<Void> release(ReleaseStockDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "库存服务暂不可用");
            }
        };
    }
}
