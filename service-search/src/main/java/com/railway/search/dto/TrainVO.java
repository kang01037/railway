package com.railway.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车次 VO（本地副本，避免跨模块依赖）。
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
