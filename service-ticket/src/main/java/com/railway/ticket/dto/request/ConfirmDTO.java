package com.railway.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 确认出票（支付成功后）：order 状态机 0 → 1。
 */
@Data
public class ConfirmDTO {

    @NotBlank
    private String orderNo;
}
