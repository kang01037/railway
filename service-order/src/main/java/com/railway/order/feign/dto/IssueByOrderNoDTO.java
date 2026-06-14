package com.railway.order.feign.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Feign 调 service-ticket /tickets/issue 的入参（支付成功后出票）。
 */
@Data
public class IssueByOrderNoDTO {

    @NotBlank
    private String orderNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;
}
