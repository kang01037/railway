package com.railway.trainstock.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 库存扣减确认请求。支付成功后调用，**Redis 不再回滚**（库存已被预占），
 * DB 把 remain 真正扣减（之前是"预占"，DB 还没扣）。
 */
@Data
public class ConfirmDTO {

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
