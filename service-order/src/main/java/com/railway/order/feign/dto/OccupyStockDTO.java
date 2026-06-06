package com.railway.order.feign.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * Feign 调 service-train-stock /stock/occupy 的入参。
 * 字段与 service-train-stock 的 OccupyDTO 完全一致（JSON 契约）。
 */
@Data
public class OccupyStockDTO {

    @NotBlank
    private String orderNo;

    @NotBlank
    private String trainNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    @NotBlank
    @Pattern(regexp = "^(BUSINESS|FIRST|SECOND|STAND)$")
    private String seatType;

    @NotNull
    @Min(value = 1)
    private Integer num;
}
