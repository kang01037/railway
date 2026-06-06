package com.railway.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 加载 resources/lua/ 下的 Lua 脚本为 Spring Bean。
 * 业务模块注入 {@code DefaultRedisScript<Long>} 后通过
 * {@code redisTemplate.execute(script, keys, args)} 调用。
 */
@Configuration
public class RedisLuaConfig {

    @Bean
    public DefaultRedisScript<Long> occupyStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/occupy_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> releaseStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/release_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> idempotentSetScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/idempotent_set.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
