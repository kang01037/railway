package com.railway.order.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
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
 * 创建订单请求。
 * <p>约束：{@code num} = {@code passengerIds.size()}；单笔订单最多 5 人。
 */
@Data
public class CreateOrderDTO {

    @NotBlank
    private String trainNo;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate runDate;

    @NotBlank
    @Pattern(regexp = "^(BUSINESS|FIRST|SECOND|STAND)$")
    private String seatType;

    @NotEmpty
    @Size(min = 1, max = 5, message = "乘车人 1~5 人")
    private List<Long> passengerIds;

    @NotNull
    @Min(value = 0, message = "票价不能为负")
    private BigDecimal price;
}
