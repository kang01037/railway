package com.railway.trainstock.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 座位库存表 {@code train_seat_stock} 实体。
 * 唯一键：{@code (train_no, run_date, seat_type)}。
 */
@Data
public class TrainSeatStockDO {

    private Long id;

    private String trainNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    /** BUSINESS / FIRST / SECOND / STAND */
    private String seatType;

    private Integer total;
    private Integer remain;

    /** 乐观锁版本号，每次更新 +1 */
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
