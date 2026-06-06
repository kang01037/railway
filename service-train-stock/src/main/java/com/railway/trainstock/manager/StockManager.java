package com.railway.trainstock.manager;

import com.railway.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存管理：封装 Redis 缓存 + Lua 原子调用。
 *
 * <p>对业务层屏蔽 Redis key 拼装、Lua 脚本调用、TTL 等细节。
 *
 * <p>Lua 返回值约定：
 * <ul>
 *   <li>{@code 1} - 成功</li>
 *   <li>{@code 0} - 库存不足</li>
 *   <li>{@code -1} - key 不存在</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> occupyStockScript;
    private final DefaultRedisScript<Long> releaseStockScript;

    /** Redis 库存 key TTL：1 天（按出发日，够用了） */
    private static final Duration STOCK_TTL = Duration.ofDays(1);

    /**
     * 懒加载：key 不存在时用 {@code remain} 初始化（NX + TTL）。
     * <p>已存在则不覆盖。
     *
     * @return {@code true} 已加载或本就存在；{@code false} 入参 remain 为 null（无库存）
     */
    public boolean ensureStockKey(String trainNo, LocalDate runDate, String seatType, Integer remain) {
        String key = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key,
                remain == null ? "0" : String.valueOf(remain), STOCK_TTL);
        log.debug("ensureStockKey key={} remain={} ok={}", key, remain, ok);
        return Boolean.TRUE.equals(ok);
    }

    /** 当前 Redis 中的库存值（不存在返回 null） */
    public Long getCurrent(String trainNo, LocalDate runDate, String seatType) {
        String key = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        String v = stringRedisTemplate.opsForValue().get(key);
        return v == null ? null : Long.parseLong(v);
    }

    /**
     * 原子预占。需先确保 key 已存在。
     *
     * @return 1 成功，0 库存不足，-1 key 不存在
     */
    public long occupy(String trainNo, LocalDate runDate, String seatType, int num) {
        String key = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        Long r = stringRedisTemplate.execute(occupyStockScript, List.of(key), String.valueOf(num));
        return r == null ? -1L : r;
    }

    /**
     * 原子释放。
     *
     * @return 1 成功，-1 key 不存在（视为幂等成功，仍返回 1 给业务层）
     */
    public long release(String trainNo, LocalDate runDate, String seatType, int num) {
        String key = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        Long r = stringRedisTemplate.execute(releaseStockScript, List.of(key), String.valueOf(num));
        return r == null ? -1L : r;
    }
}
