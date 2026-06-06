package com.railway.order.aspect;

import com.railway.common.constant.HeaderConstant;
import com.railway.common.constant.RedisKeyConstant;
import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 幂等切面：方法上加 {@link Idempotent} 后，从请求头 {@code X-Idempotent-Key} 读 key，
 * 调 Redis Lua SETNX（idempotent_set.lua），失败抛 {@code IDEMPOTENT_REPEAT}。
 *
 * <p>Key 命名空间：{@code IDEMPOTENT:{key}}。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> idempotentSetScript;

    @Around("@annotation(com.railway.order.aspect.Idempotent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 1. 读 header
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // 非 web 环境（内部调用）→ 跳过
            return pjp.proceed();
        }
        String key = attrs.getRequest().getHeader(HeaderConstant.X_IDEMPOTENT_KEY);
        if (key == null || key.isBlank()) {
            // 无 key → 跳过幂等校验（业务层应自己保证）
            return pjp.proceed();
        }

        // 2. 拿 TTL
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Idempotent anno = sig.getMethod().getAnnotation(Idempotent.class);
        int ttl = anno == null ? 60 : anno.ttlSeconds();

        // 3. Redis SETNX
        String redisKey = RedisKeyConstant.IDEMPOTENT + key;
        Long result = stringRedisTemplate.execute(
                idempotentSetScript, List.of(redisKey), String.valueOf(ttl));
        if (result == null || result == 0L) {
            log.warn("幂等命中（重复请求）key={}", key);
            throw new BizException(ErrorCode.IDEMPOTENT_REPEAT, "重复请求，请勿重试");
        }
        return pjp.proceed();
    }
}
