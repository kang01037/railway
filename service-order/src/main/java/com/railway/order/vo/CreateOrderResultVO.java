package com.railway.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单成功返回：orderNo + 支付 URL + 过期时间。
 */
@Data
public class CreateOrderResultVO {

    private String orderNo;
    private String payUrl;
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
