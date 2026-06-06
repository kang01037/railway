package com.railway.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 乘车人新增/修改请求 DTO。
 */
@Data
public class PassengerReq {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不超过 50")
    private String name;

    @NotBlank(message = "证件号不能为空")
    @Size(min = 15, max = 32, message = "证件号长度 15-32")
    private String idCardNo;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 0-成人 1-儿童 2-学生 */
    private Integer passengerType;

    /** 0-否 1-默认乘车人 */
    private Integer isDefault;
}
