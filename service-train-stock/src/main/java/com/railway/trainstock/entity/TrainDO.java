package com.railway.trainstock.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车次表 {@code train} 实体。
 */
@Data
public class TrainDO {

    private Long id;

    /** 车次号，唯一，例如 G1234 */
    private String trainNo;

    /** G / D / K / T */
    private String trainType;

    private String startStation;
    private String endStation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 周一~周日 0/1，例 {@code 1111111} 表示每天都开 */
    private String runDays;

    /** 0-停运 1-正常 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
