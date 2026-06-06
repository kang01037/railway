package com.railway.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退票：order 状态机 0/1 → 3。
 */
@Data
public class CancelDTO {

    @NotBlank
    private String orderNo;
}
