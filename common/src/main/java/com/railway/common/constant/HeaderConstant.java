package com.railway.common.constant;

/**
 * HTTP Header 常量。网关与业务服务之间用 X-User-* 透传当前用户信息，
 * 业务服务不再解析 JWT，直接读 X-User-Id 等。
 */
public final class HeaderConstant {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USER_NAME = "X-User-Name";
    public static final String X_USER_ROLES = "X-User-Roles";

    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_IDEMPOTENT_KEY = "X-Idempotent-Key";

    private HeaderConstant() {
    }
}
