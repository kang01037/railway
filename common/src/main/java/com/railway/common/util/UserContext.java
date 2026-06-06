package com.railway.common.util;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;

/**
 * 当前登录用户的 ThreadLocal 工具。
 *
 * <p>由 {@link com.railway.common.interceptor.AuthInterceptor} 在请求进入时 set，结束时 clear。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    public static void set(LoginUser user) {
        CURRENT.set(user);
    }

    public static LoginUser get() {
        return CURRENT.get();
    }

    /** 别名，等价于 {@link #get()} */
    public static LoginUser current() {
        return CURRENT.get();
    }

    /** 当前用户 id；未登录返回 null */
    public static Long currentUserId() {
        LoginUser u = CURRENT.get();
        return u == null ? null : u.getUserId();
    }

    /** 当前用户 id；未登录抛 UNAUTHORIZED */
    public static Long mustCurrentUserId() {
        LoginUser u = CURRENT.get();
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return u.getUserId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
