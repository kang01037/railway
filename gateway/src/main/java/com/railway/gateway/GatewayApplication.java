package com.railway.gateway;

import com.railway.gateway.config.GatewayAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动入口。
 *
 * <p>职责：路由分发（lb://service-*）+ JWT 鉴权透传 + TraceId 注入 + CORS。
 * <p>不写业务代码；不连接 MySQL/Redis。
 *
 * <p>排除 {@link DataSourceAutoConfiguration}：common 传递引入 {@code spring-boot-starter-web}，
 * 触发 Hikari DataSource 自动配置，但 gateway 不需要 DB。
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
