package com.railway.search.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRemainVO {

    private String seatType;        // BUSINESS/FIRST/SECOND/STAND
    private java.math.BigDecimal price;
    private Integer total;
    private Integer remain;
}
