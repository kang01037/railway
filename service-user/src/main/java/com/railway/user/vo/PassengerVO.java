package com.railway.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 乘车人返回 VO。身份证号**已脱敏**（前 6 后 4）。
 */
@Data
public class PassengerVO {

    private Long id;
    private Long userId;
    private String name;
    private Integer idCardType;
    private String idCardNo;       // 脱敏后：前6 + "********" + 后4
    private String phone;
    private Integer passengerType;
    private Integer isDefault;
    private LocalDateTime createTime;
}
