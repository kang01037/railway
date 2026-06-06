package com.railway.order.feign;

import com.railway.common.model.R;
import com.railway.order.feign.dto.CreatePayDTO;
import com.railway.order.feign.dto.PayVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign 调 service-payment（创建支付单 / 查支付单）。
 */
@FeignClient(name = "service-payment", path = "/pay",
        fallbackFactory = PaymentFeignClientFallback.class)
public interface PaymentFeignClient {

    @PostMapping("/create")
    R<PayVO> create(@RequestBody CreatePayDTO dto);

    @GetMapping("/{payNo}")
    R<PayVO> getByPayNo(@PathVariable String payNo);
}
