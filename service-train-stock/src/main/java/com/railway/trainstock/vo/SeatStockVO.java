package com.railway.trainstock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单条座位库存的 VO（车次详情页用）。
 */
@Data
public class SeatStockVO {

    private Long id;
    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    private String seatType;
    private java.math.BigDecimal price;
    private Integer total;
    private Integer remain;
}
