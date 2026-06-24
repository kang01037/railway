package com.railway.search.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车次搜索响应 DTO（MySQL + Redis 实时数据，无 ES 依赖）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainIndex {

    private String id;
    private String trainNo;
    private String trainType;                       // G/D/K/T
    private String startStation;
    private String endStation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String runDays;                         // "1111111"
    private LocalDate runDate;
    private List<SeatPrice> prices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatPrice {
        private String seatType;                    // BUSINESS/FIRST/SECOND/STAND
        private Double price;
        private Integer total;                      // 总票数（实时）
        private Integer remain;                     // 余票数（实时）
    }
}
