package com.railway.order.feign.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Feign 调 service-payment /pay/create 返回的 VO 字段。
 * 字段名严格匹配 service-payment PayVO。
 */
@Data
public class PayVO {

    private String payNo;
    private String orderNo;
    private BigDecimal amount;
    private String payChannel;
    private String payUrl;
    private Integer status;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
}
