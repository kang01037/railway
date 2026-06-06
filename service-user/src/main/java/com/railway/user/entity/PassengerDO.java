package com.railway.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 乘车人实体，对应表 {@code passenger}。
 */
@Data
public class PassengerDO {

    private Long id;
    private Long userId;
    private String name;
    private Integer idCardType;
    private String idCardNo;
    private String phone;
    private Integer passengerType;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
