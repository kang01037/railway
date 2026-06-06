package com.railway.order.feign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Feign 调 service-ticket /tickets/cancel 的入参。
 */
@Data
public class CancelTicketDTO {
    @NotBlank
    private String orderNo;
}
