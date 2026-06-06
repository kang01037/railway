package com.railway.payment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表 {@code pay_record} 实体。
 * <p>{@code payNo} 业务单号（{@code P} + 雪花 ID），唯一。
 * <p>{@code payChannel} ALIPAY / WECHAT / SIM（demo 阶段用 SIM 模拟）。
 * <p>{@code status} 0-待支付 1-成功 2-失败 3-已关闭。
 */
@Data
public class PayRecordDO {

    private Long id;

    private String payNo;
    private String orderNo;
    private BigDecimal amount;
    private String payChannel;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
