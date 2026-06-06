package com.railway.common.interceptor;

import com.railway.common.annotation.AuthIgnore;
import com.railway.common.constant.HeaderConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;
import com.railway.common.util.JwtUtil;
import com.railway.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：
 * <ol>
 *   <li>标记 {@link AuthIgnore} 的方法 / 类 - 直接放行</li>
 *   <li>非 HandlerMethod（静态资源等）- 放行</li>
 *   <li>读 {@code Authorization} 头 → 去 Bearer 前缀 → {@link JwtUtil#parse(String)} → 写 UserContext</li>
 *   <li>缺头 / 解析失败 → 抛 {@link BizException}(UNAUTHORIZED)</li>
 * </ol>
 *
 * <p>请求结束（{@link #afterCompletion}）清空 UserContext，避免线程复用造成内存泄漏。
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        if (hm.getMethodAnnotation(AuthIgnore.class) != null
                || hm.getBeanType().getAnnotation(AuthIgnore.class) != null) {
            return true;
        }
        String authHeader = request.getHeader(HeaderConstant.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(HeaderConstant.BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少 Authorization 头");
        }
        String token = authHeader.substring(HeaderConstant.BEARER_PREFIX.length()).trim();
        LoginUser user = jwtUtil.parse(token);
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
