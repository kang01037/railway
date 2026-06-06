package com.railway.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解：要求当前登录用户至少拥有其中一个角色。
 *
 * <p>例：{@code @RequireRole({"ADMIN"})}  - 用户角色集合需包含 ADMIN。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String[] value();
}
