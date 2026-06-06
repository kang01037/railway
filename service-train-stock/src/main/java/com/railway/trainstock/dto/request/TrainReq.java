package com.railway.trainstock.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增 / 修改车次请求。
 * <p>新增时可一次性带上若干座位类型的初始库存（{@link InitialStock}）。
 */
@Data
public class TrainReq {

    @NotBlank(message = "车次号不能为空")
    @Size(max = 20)
    private String trainNo;

    @NotBlank(message = "车次类型不能为空")
    @Pattern(regexp = "^(G|D|K|T)$", message = "车次类型仅支持 G/D/K/T")
    private String trainType;

    @NotBlank
    @Size(max = 50)
    private String startStation;

    @NotBlank
    @Size(max = 50)
    private String endStation;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Pattern(regexp = "^[01]{7}$", message = "runDays 必须是 7 位 0/1 串")
    private String runDays;

    /** 0-停运 1-正常；新增时可不传，默认 1 */
    private Integer status;

    /** 初始库存：仅新增时使用。例：[{"runDate":"2026-06-15","seatType":"BUSINESS","total":20}] */
    @Valid
    @NotEmpty(message = "请至少配置一种座位库存")
    private List<InitialStock> initialStocks;

    @Data
    public static class InitialStock {
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate runDate;

        @NotBlank
        @Pattern(regexp = "^(BUSINESS|FIRST|SECOND|STAND)$", message = "座位类型仅支持 BUSINESS/FIRST/SECOND/STAND")
        private String seatType;

        @NotNull
        @Min(value = 0, message = "总票数不能为负")
        private Integer total;
    }
}
