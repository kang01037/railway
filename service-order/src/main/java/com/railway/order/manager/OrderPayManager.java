package com.railway.order.manager;

import com.railway.common.model.R;
import com.railway.order.feign.PaymentFeignClient;
import com.railway.order.feign.dto.CreatePayDTO;
import com.railway.order.feign.dto.PayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付编排：调 payment.create 拿 payUrl。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPayManager {

    private final PaymentFeignClient paymentFeignClient;

    public PayVO createPay(String orderNo, BigDecimal amount) {
        CreatePayDTO dto = new CreatePayDTO();
        dto.setOrderNo(orderNo);
        dto.setAmount(amount);
        dto.setPayChannel("SIM");
        R<PayVO> r = paymentFeignClient.create(dto);
        if (r == null || r.getCode() != 200 || r.getData() == null) {
            throw new com.railway.common.exception.BizException(
                    com.railway.common.exception.ErrorCode.PAY_FAILED,
                    r == null ? "payment.create 失败（无响应）" : r.getMessage());
        }
        return r.getData();
    }
}
