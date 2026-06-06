package com.railway.order.feign.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Feign 调 service-ticket /tickets/issue 的入参。
 */
@Data
public class IssueTicketDTO {

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
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @Valid
    @NotEmpty
    private List<PassengerItem> passengers;

    @Data
    public static class PassengerItem {
        @NotNull
        private Long passengerId;
        @NotBlank
        private String passengerName;
        @NotBlank
        private String idCardNo;
    }
}
