package com.railway.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 {@code user}。
 */
@Data
public class UserDO {

    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String idCard;
    private String realName;
    private Integer userType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
