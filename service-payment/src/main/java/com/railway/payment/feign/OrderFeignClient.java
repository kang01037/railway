package com.railway.payment.feign;

import com.railway.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign 调 service-order（支付成功后同步确认订单）。
 */
@FeignClient(name = "service-order", path = "/orders",
        fallbackFactory = OrderFeignClientFallback.class)
public interface OrderFeignClient {

    @PostMapping("/internal/confirm")
    R<Integer> confirm(@RequestParam("orderNo") String orderNo);
}
