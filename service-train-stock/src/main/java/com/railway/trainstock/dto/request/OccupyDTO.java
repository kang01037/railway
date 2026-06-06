package com.railway.trainstock.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 库存预占请求。**服务间内部接口**（被 service-order 通过 Feign 调用），
 * 不走 gateway 鉴权。{@code orderNo} 必填（写库存流水用）。
 */
@Data
public class OccupyDTO {

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
    @Min(value = 1, message = "预占数量必须 ≥ 1")
    private Integer num;
}
