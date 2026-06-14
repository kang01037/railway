package com.railway.ticket.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 支付成功后出票（仅传 orderNo + runDate，ticket 数据已在 preAllocate 时写入 MySQL）。
 */
@Data
public class IssueByOrderNoDTO {

    @NotBlank
    private String orderNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;
}
