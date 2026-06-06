package com.railway.payment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 VO。{@code payUrl} 供前端跳转 / 轮询。
 */
@Data
public class PayVO {

    private String payNo;
    private String orderNo;
    private BigDecimal amount;
    private String payChannel;
    private String payUrl;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
