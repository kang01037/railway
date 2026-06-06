package com.railway.gateway.filter;

import com.railway.common.constant.HeaderConstant;
import com.railway.common.model.LoginUser;
import com.railway.common.util.JwtUtil;
import com.railway.gateway.config.GatewayAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 全局鉴权过滤器（reactive）。
 *
 * <ol>
 *   <li>白名单（Ant 风格）→ 直接放行。</li>
 *   <li>读 {@code Authorization: Bearer xxx}。</li>
 *   <li>{@link JwtUtil#parse(String)} 解析失败 → 401 JSON。</li>
 *   <li>解析成功 → 把 {@code userId / username / roles} 写入 {@code X-User-Id / X-User-Name / X-User-Roles} 头透传下游。</li>
 * </ol>
 *
 * <p>Order = HIGHEST_PRECEDENCE + 10（TraceId 之后）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final GatewayAuthProperties props;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();

        // 1. 白名单
        for (String pattern : props.getPublicPaths()) {
            if (MATCHER.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 读 Authorization
        List<String> auths = req.getHeaders().get(HeaderConstant.AUTHORIZATION);
        if (auths == null || auths.isEmpty() || !StringUtils.hasText(auths.get(0))) {
            return unauthorized(exchange, "缺少 Authorization 头");
        }
        String token = auths.get(0);
        if (!token.startsWith(HeaderConstant.BEARER_PREFIX)) {
            return unauthorized(exchange, "Authorization 头格式错误");
        }
        token = token.substring(HeaderConstant.BEARER_PREFIX.length()).trim();

        // 3. 解析
        LoginUser user;
        try {
            user = jwtUtil.parse(token);
        } catch (Exception e) {
            log.warn("JWT 解析失败 path={} msg={}", path, e.getMessage());
            return unauthorized(exchange, e.getMessage() == null ? "Token 无效" : e.getMessage());
        }

        // 4. 注入头
        String rolesCsv = (user.getRoles() == null || user.getRoles().isEmpty())
                ? "" : String.join(",", user.getRoles());

        ServerHttpRequest mutated = req.mutate()
                .header(HeaderConstant.X_USER_ID, String.valueOf(user.getUserId()))
                .header(HeaderConstant.X_USER_NAME, user.getUsername() == null ? "" : user.getUsername())
                .header(HeaderConstant.X_USER_ROLES, rolesCsv)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 最小 JSON：和 R 一致；R.traceId 由 TraceIdFilter 设置响应头时附带
        String body = "{\"code\":401,\"message\":\""
                + msg.replace("\"", "\\\"")
                + "\",\"data\":null}";
        DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Mono.just(buf));
    }
}
