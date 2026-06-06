package com.railway.gateway.filter;

import cn.hutool.core.util.IdUtil;
import com.railway.common.constant.HeaderConstant;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局 TraceId 过滤器。
 *
 * <ol>
 *   <li>读请求头 {@code X-Trace-Id}；缺失则生成 UUID。</li>
 *   <li>写入下游请求头（透传）。</li>
 *   <li>写入响应头（前端可拿）。</li>
 * </ol>
 *
 * <p>Order = HIGHEST_PRECEDENCE，最先执行。
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String traceId = req.getHeaders().getFirst(HeaderConstant.X_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = IdUtil.fastUUID();
        }

        ServerHttpRequest mutated = req.mutate()
                .header(HeaderConstant.X_TRACE_ID, traceId)
                .build();

        ServerHttpResponse resp = exchange.getResponse();
        resp.getHeaders().add(HeaderConstant.X_TRACE_ID, traceId);

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
