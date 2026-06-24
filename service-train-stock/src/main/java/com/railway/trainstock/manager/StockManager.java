package com.railway.trainstock.manager;

import com.railway.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 库存管理：封装 Redisson 分布式锁 + Redis 原子操作。
 *
 * <p>使用 Redisson 的 RLock 替代 Lua 脚本中的 SETNX 分布式锁，
 * 提供更可靠的锁机制（自动续期、可重入、公平锁等特性）。
 *
 * <p>返回值约定：
 * <ul>
 *   <li>{@code 1} - 成功</li>
 *   <li>{@code 0} - 库存不足</li>
 *   <li>{@code -1} - 库存 key 不存在</li>
 *   <li>{@code -2} - 获取锁失败（需重试）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    /** Redis 库存 key TTL：1 天（按出发日，够用了） */
    private static final Duration STOCK_TTL = Duration.ofDays(1);

    /** 分布式锁等待时间（秒） */
    private static final long LOCK_WAIT_SECONDS = 3;

    /** 分布式锁自动释放时间（秒），Redisson 会自动续期 */
    private static final long LOCK_LEASE_SECONDS = 10;

    /**
     * 懒加载：key 不存在则用 {@code remain} 初始化（NX + TTL）。
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
     * Redisson 分布式锁 + 原子预占。需先确保 key 已存在。
     * <p>使用 Redisson RLock 替代 Lua 脚本中的 SETNX，提供更可靠的锁机制。
     *
     * @return 1 成功，0 库存不足，-1 key 不存在，-2 获取锁失败
     */
    public long occupy(String trainNo, LocalDate runDate, String seatType, int num) {
        String stockKey = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        String lockKey = RedisKeyConstant.stockLockKey(trainNo, runDate.toString(), seatType);

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，等待 3 秒，锁自动释放时间 10 秒（Redisson 会自动续期）
            boolean acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("occupy 获取锁超时 key={}", stockKey);
                return -2;
            }

            try {
                // 检查库存 key 是否存在
                String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
                if (stockStr == null) {
                    return -1;
                }

                long stock = Long.parseLong(stockStr);
                if (stock < num) {
                    return 0;
                }

                // 原子扣减
                stringRedisTemplate.opsForValue().decrement(stockKey, num);
                return 1;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("occupy 获取锁被中断 key={}", stockKey, e);
            return -2;
        }
    }

    /**
     * Redisson 分布式锁 + 原子释放。
     * <p>使用 Redisson RLock 替代 Lua 脚本中的 SETNX，提供更可靠的锁机制。
     *
     * @return 1 成功，-1 key 不存在（视为幂等成功），-2 获取锁失败
     */
    public long release(String trainNo, LocalDate runDate, String seatType, int num) {
        String stockKey = RedisKeyConstant.stockKey(trainNo, runDate.toString(), seatType);
        String lockKey = RedisKeyConstant.stockLockKey(trainNo, runDate.toString(), seatType);

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            boolean acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("release 获取锁超时 key={}", stockKey);
                return -2;
            }

            try {
                // 检查库存 key 是否存在
                String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
                if (stockStr == null) {
                    return -1;
                }

                // 原子增加
                stringRedisTemplate.opsForValue().increment(stockKey, num);
                return 1;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("release 获取锁被中断 key={}", stockKey, e);
            return -2;
        }
    }
}
