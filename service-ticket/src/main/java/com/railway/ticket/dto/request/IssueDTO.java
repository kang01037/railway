package com.railway.ticket.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出票请求（Feign 入参，被 service-order 调）。
 * <p>一张订单对应 N 张票（{@code passengers.size()} = N）。
 */
@Data
public class IssueDTO {

    @NotBlank
    private String orderNo;

    @NotBlank
    private String trainNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    @NotBlank
    @Pattern(regexp = "^(BUSINESS|FIRST|SECOND|STAND)$")
    private String seatType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "价格必须 > 0")
    private BigDecimal price;

    @Valid
    @NotEmpty(message = "乘车人不能为空")
    private List<PassengerItem> passengers;

    @Data
    public static class PassengerItem {
        @NotNull
        private Long passengerId;

        @NotBlank
        @Size(max = 50)
        private String passengerName;

        @NotBlank
        @Size(max = 32)
        private String idCardNo;
    }
}
