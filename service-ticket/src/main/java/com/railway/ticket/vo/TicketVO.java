package com.railway.ticket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 票详情 VO。{@code idCardNo} 已脱敏。
 */
@Data
public class TicketVO {

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
    private String idCardNo;     // masked by IdCardValidator.mask

    private BigDecimal price;

    /** 0-待支付 1-已出票 2-已改签 3-已退 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
