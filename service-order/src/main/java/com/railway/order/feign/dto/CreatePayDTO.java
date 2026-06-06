package com.railway.order.feign.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Feign 调 service-payment /pay/create 的入参。
 */
@Data
public class CreatePayDTO {

    @NotBlank
    private String orderNo;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^(ALIPAY|WECHAT|SIM)$")
    private String payChannel;
}
