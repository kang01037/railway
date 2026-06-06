package com.railway.order.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等键注解。方法上加此注解，框架读请求头 {@code X-Idempotent-Key}，
 * 执行前 Redis SETNX（Lua 原子），失败抛 {@code IDEMPOTENT_REPEAT}。
 *
 * <p>典型场景：下单接口（避免双击 / 客户端重试导致重复扣库存）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等键 TTL（秒），默认 60s */
    int ttlSeconds() default 60;
}
