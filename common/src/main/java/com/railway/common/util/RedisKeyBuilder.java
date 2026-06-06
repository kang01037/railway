package com.railway.common.util;

/**
 * Redis key 拼接工具：prefix + part1 + part2 + ...
 *
 * <p>使用 {@code ':'} 作为分隔符，便于在 Redis Desktop Manager / redis-cli 中按前缀分组。
 */
public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String build(String prefix, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        if (parts != null) {
            for (Object p : parts) {
                sb.append(':').append(p);
            }
        }
        return sb.toString();
    }
}
