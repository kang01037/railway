package com.railway.trainstock.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 库存释放请求。订单取消 / 支付失败时调用，Redis + DB 均回滚。
 */
@Data
public class ReleaseDTO {

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
