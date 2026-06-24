package com.railway.payment.feign;

import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 订单服务降级：调用失败 → R.fail(SERVER_ERROR)。
 * 防止 order 宕机时整个支付链雪崩。
 */
@Slf4j
@Component
public class OrderFeignClientFallback implements FallbackFactory<OrderFeignClient> {

    @Override
    public OrderFeignClient create(Throwable cause) {
        log.error("OrderFeignClient 降级 cause={}", cause.getMessage(), cause);
        return new OrderFeignClient() {
            @Override
            public R<Integer> confirm(String orderNo) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "订单服务暂不可用");
            }
        };
    }
}
