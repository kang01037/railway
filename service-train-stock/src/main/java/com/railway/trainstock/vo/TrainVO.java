package com.railway.trainstock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车次 VO。
 */
@Data
public class TrainVO {

    private Long id;
    private String trainNo;
    private String trainType;
    private String startStation;
    private String endStation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String runDays;
    private Integer status;
}
