package com.railway.gateway.config;

import com.railway.common.util.JwtUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
