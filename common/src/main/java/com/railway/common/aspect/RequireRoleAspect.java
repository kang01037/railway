package com.railway.common.aspect;

import com.railway.common.annotation.RequireRole;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import com.railway.common.model.LoginUser;
import com.railway.common.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Set;

/**
 * 角色权限切面：从 UserContext 取当前用户的 roles，与 RequireRole 指定的角色取并集判断。
 */
@Slf4j
@Aspect
public class RequireRoleAspect {

    @Before("@annotation(requireRole)")
    public void check(JoinPoint joinPoint, RequireRole requireRole) {
        LoginUser user = UserContext.current();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Set<String> userRoles = user.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            throw new BizException(ErrorCode.FORBIDDEN, "用户无任何角色");
        }
        for (String required : requireRole.value()) {
            if (userRoles.contains(required)) {
                return;
            }
        }
        log.warn("用户 {} 缺少角色 {}, 当前 roles={}",
                user.getUserId(), java.util.Arrays.toString(requireRole.value()), userRoles);
        throw new BizException(ErrorCode.FORBIDDEN, "缺少所需角色");
    }
}
