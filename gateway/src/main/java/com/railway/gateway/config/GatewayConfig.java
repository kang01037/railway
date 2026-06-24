package com.railway.gateway.config;

import com.railway.common.util.JwtUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关专属配置。
 *
 * <p>common 的 {@code WebAutoConfiguration} 带
 * {@code @ConditionalOnWebApplication(SERVLET)}，不会在 webflux 环境下加载；
 * 网关需要的 {@link JwtUtil} {@code @Bean} 在此显式声明。
 */
@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayConfig {

    @Bean
    @ConfigurationProperties(prefix = "jwt")
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    /**
     * 限流 KeyResolver：按用户 ID 限流。
     * <p>从 JWT 解析 userId，未登录则按 IP 限流。
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 尝试从请求头获取 userId（AuthGlobalFilter 已解析 JWT 并设置）
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            // 未登录用户按 IP 限流
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
