package com.railway.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置（{@code gateway.*}）。
 *
 * <ul>
 *   <li>{@code public-paths}：Ant 风格路径白名单（如 {@code /api/user/auth/login}、{@code /api/payment/callback/**}）。</li>
 *   <li>{@code ignored-methods}：HTTP 方法白名单（如 {@code GET}），不推荐使用，保留扩展。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "gateway-auth")
public class GatewayAuthProperties {

    /** 白名单路径（Ant 风格，命中即放行） */
    private List<String> publicPaths = new ArrayList<>();
}
