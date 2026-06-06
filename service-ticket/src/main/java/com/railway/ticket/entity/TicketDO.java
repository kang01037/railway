package com.railway.ticket.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 票表 {@code ticket} 实体。
 * <ul>
 *   <li>{@code ticketNo} 业务票号（{@code T} + 雪花 ID），唯一</li>
 *   <li>{@code price} DECIMAL(10,2)，冗余单价</li>
 *   <li>{@code status} 0-待支付 1-已出票 2-已改签 3-已退</li>
 *   <li>{@code idCardNo} / {@code passengerName} 冗余，下单时快照</li>
 * </ul>
 */
@Data
public class TicketDO {

    private Long id;

    private String ticketNo;
    private String orderNo;
    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    private String seatType;

    private Integer carriageNo;

    private String seatNo;

    private Long passengerId;
    private String passengerName;
    private String idCardNo;

    private BigDecimal price;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
