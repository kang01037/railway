package com.railway.trainstock.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存流水表 {@code stock_flow} 实体。
 * <p>{@code delta} 正数=释放 负数=预占/扣减；
 * {@code bizType} 1-预占 2-确认 3-释放。
 */
@Data
public class StockFlowDO {

    private Long id;

    private String orderNo;
    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    private String seatType;
    private Integer delta;
    private Integer bizType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
