package com.railway.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单返回：orderNo + 状态 + 支付 URL + 过期时间。
 * <p>status: 4-排队中 0-待支付（可直接支付）
 */
@Data
public class CreateOrderResultVO {

    private String orderNo;
    private Integer status;  // 4-排队中 0-待支付
    private String payUrl;
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
