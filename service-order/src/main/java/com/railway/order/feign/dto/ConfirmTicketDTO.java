package com.railway.order.feign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Feign 调 service-ticket /tickets/confirm 的入参。
 */
@Data
public class ConfirmTicketDTO {
    @NotBlank
    private String orderNo;
}
