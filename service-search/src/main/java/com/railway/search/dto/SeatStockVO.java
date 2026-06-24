package com.railway.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 座位库存 VO（本地副本，避免跨模块依赖）。
 */
@Data
public class SeatStockVO {

    private Long id;
    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    private String seatType;
    private BigDecimal price;
    private Integer total;
    private Integer remain;
}
