package com.railway.ticket.manager;

import com.railway.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * 座位池管理：Redis Set 存储 (trainNo, runDate, seatType) 的可用座位号。
 * <p>用 {@code SADD 1..total} 初始化；{@code SPOP} 拿一个；{@code SADD seat} 还回去。
 *
 * <p>座位号 demo 阶段用纯数字字符串（{@code "1" "2" "3" ...}），carriageNo 统一为 1。
 * 后续要细化（"1A"/"1B"/多车厢）只改本类即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatPoolManager {

    /** 座位池 TTL：1 天（按出发日） */
    private static final Duration POOL_TTL = Duration.ofDays(1);

    /** 默认车厢号（demo 简化） */
    private static final int DEFAULT_CARRIAGE = 1;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 懒加载座位池：仅当 key 不存在时初始化 {@code total} 个座位（{@code 1..total}）。
     */
    public void initIfAbsent(String trainNo, LocalDate runDate, String seatType, int total) {
        String key = RedisKeyConstant.seatKey(trainNo, runDate.toString(), seatType);
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }
        Set<String> members = new HashSet<>(total);
        for (int i = 1; i <= total; i++) {
            members.add(String.valueOf(i));
        }
        Long added = stringRedisTemplate.opsForSet().add(key, members.toArray(new String[0]));
        stringRedisTemplate.expire(key, POOL_TTL);
        log.debug("initIfAbsent seat pool key={} total={} added={}", key, total, added);
    }

    /**
     * 从座位池原子弹出一个座位号（{@code SPOP}）。
     *
     * @return 座位号；池空返回 null
     */
    public String pop(String trainNo, LocalDate runDate, String seatType) {
        String key = RedisKeyConstant.seatKey(trainNo, runDate.toString(), seatType);
        return stringRedisTemplate.opsForSet().pop(key);
    }

    /**
     * 退还座位号（{@code SADD}），幂等。
     */
    public void push(String trainNo, LocalDate runDate, String seatType, String seatNo) {
        if (seatNo == null || seatNo.isBlank()) {
            return;
        }
        String key = RedisKeyConstant.seatKey(trainNo, runDate.toString(), seatType);
        stringRedisTemplate.opsForSet().add(key, seatNo);
    }

    public int defaultCarriage() {
        return DEFAULT_CARRIAGE;
    }
}
