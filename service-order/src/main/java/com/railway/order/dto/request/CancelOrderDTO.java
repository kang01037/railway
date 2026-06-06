package com.railway.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单请求（仅本人可取消自己的订单）。
 */
@Data
public class CancelOrderDTO {

    @NotBlank
    private String orderNo;

    @Size(max = 200)
    private String reason;
}
