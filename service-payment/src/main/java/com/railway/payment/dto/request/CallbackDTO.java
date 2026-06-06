package com.railway.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付回调（模拟）。{@code payNo} 来自 createPay 返回，{@code success} 表支付结果。
 */
@Data
public class CallbackDTO {

    @NotBlank
    private String payNo;

    @NotNull
    private Boolean success;
}
