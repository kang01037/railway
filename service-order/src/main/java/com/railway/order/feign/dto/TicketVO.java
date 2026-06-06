package com.railway.order.feign.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Feign 调 service-ticket /tickets/order/{orderNo} 返回的 VO 字段。
 * 字段名严格匹配 service-ticket TicketVO。
 */
@Data
public class TicketVO {

    private Long id;
    private String ticketNo;
    private String orderNo;
    private String trainNo;
    private LocalDate runDate;
    private String seatType;
    private Integer carriageNo;
    private String seatNo;
    private Long passengerId;
    private String passengerName;
    private String idCardNo;        // 已脱敏
    private BigDecimal price;
    private Integer status;
}
