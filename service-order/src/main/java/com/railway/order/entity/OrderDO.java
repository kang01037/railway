package com.railway.order.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单表 {@code orders} 实体。
 * <ul>
 *   <li>{@code orderNo} 业务订单号（{@code O} + 雪花 ID），唯一</li>
 *   <li>{@code amount} DECIMAL(10,2) 总金额 = price * num</li>
 *   <li>{@code status} 0-待支付 1-已支付 2-已取消 3-已退款 4-已完成</li>
 *   <li>{@code expireTime} 支付截止时间（默认 now + 15min），超时未支付自动关单</li>
 * </ul>
 */
@Data
public class OrderDO {

    private Long id;

    private String orderNo;
    private Long userId;
    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    private String seatType;
    private Integer num;
    private BigDecimal amount;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
