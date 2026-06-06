package com.railway.order.feign;

import com.railway.common.exception.ErrorCode;
import com.railway.common.model.R;
import com.railway.order.feign.dto.CreatePayDTO;
import com.railway.order.feign.dto.PayVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentFeignClientFallback implements FallbackFactory<PaymentFeignClient> {

    @Override
    public PaymentFeignClient create(Throwable cause) {
        log.error("PaymentFeignClient 降级 cause={}", cause.getMessage(), cause);
        return new PaymentFeignClient() {
            @Override
            public R<PayVO> create(CreatePayDTO dto) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "支付服务暂不可用");
            }
            @Override
            public R<PayVO> getByPayNo(String payNo) {
                return R.fail(ErrorCode.SERVER_ERROR.getCode(), "支付服务暂不可用");
            }
        };
    }
}
