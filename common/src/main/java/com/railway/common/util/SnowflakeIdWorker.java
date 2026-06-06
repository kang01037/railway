package com.railway.common.util;

import com.railway.common.exception.BizException;
import com.railway.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法 ID 生成器：1 bit 符号 + 41 bit 时间戳 + 10 bit worker + 12 bit sequence。
 *
 * <p>可由 Spring 配置 {@code snowflake.worker-id} 注入；同一毫秒内单节点可生成 4096 个 ID。
 *
 * <p>时钟回拨保护：检测到 lastTimestamp > 当前时间时拒绝生成，调用方需自行重试或使用备用 ID 源。
 */
@Slf4j
public class SnowflakeIdWorker {

    /** 起始时间戳：2023-11-14 22:13:20 */
    private static final long EPOCH = 1700000000000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private long workerId = 1L;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdWorker() {
    }

    public SnowflakeIdWorker(long workerId) {
        setWorkerId(workerId);
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error("时钟回拨 拒绝生成 id lastTimestamp={} currentTimestamp={}", lastTimestamp, timestamp);
            throw new BizException(ErrorCode.SERVER_ERROR, "系统时钟异常");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public void setWorkerId(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "snowflake workerId 范围 [0, " + MAX_WORKER_ID + "]");
        }
        this.workerId = workerId;
    }

    public long getWorkerId() {
        return workerId;
    }
}
