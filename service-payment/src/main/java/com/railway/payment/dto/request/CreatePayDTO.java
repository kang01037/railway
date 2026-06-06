package com.railway.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起支付请求。
 * <p>{@code orderNo} + {@code amount} + {@code payChannel}。
 * 演示阶段 payChannel 推荐 {@code SIM}（模拟）。
 */
@Data
public class CreatePayDTO {

    @NotBlank
    private String orderNo;

    @NotNull
    @DecimalMin(value = "0.01", message = "支付金额必须 > 0")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^(ALIPAY|WECHAT|SIM)$", message = "支付渠道仅支持 ALIPAY/WECHAT/SIM")
    private String payChannel;
}
