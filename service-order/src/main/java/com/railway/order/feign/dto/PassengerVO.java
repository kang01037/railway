package com.railway.order.feign.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Feign 调 service-user /passengers（取某用户下的乘车人）返回的 VO 字段。
 * 字段名严格匹配 service-user PassengerVO。
 */
@Data
public class PassengerVO {

    private Long id;
    private Long userId;
    private String name;
    private Integer idCardType;
    private String idCardNo;        // 已脱敏（service-user 的 VO 已 mask）
    private String phone;
    private Integer passengerType;
    private Integer isDefault;
    private LocalDate createTime;
}
