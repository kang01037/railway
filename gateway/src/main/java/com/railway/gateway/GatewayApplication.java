package com.railway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动入口。
 *
 * <p>职责：路由分发（lb://service-*）+ JWT 鉴权透传 + TraceId 注入 + CORS。
 * <p>不写业务代码；不连接 MySQL/Redis。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
