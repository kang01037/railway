package com.railway.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 当前登录用户（存放在 {@link com.railway.common.util.UserContext} 的 ThreadLocal 中）。
 *
 * <p>由网关 / AuthInterceptor 在解析 JWT 后构造；下游服务可从 UserContext 读取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private Set<String> roles;
}
