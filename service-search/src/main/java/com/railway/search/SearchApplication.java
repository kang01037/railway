package com.railway.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ES 检索服务入口。
 *
 * <p>职责：
 * <ul>
 *   <li>{@code /search/trains}：车次条件检索（ES）</li>
 *   <li>{@code /search/trains/{trainNo}/seats}：座位余票（Feign → service-train-stock）</li>
 * </ul>
 *
 * <p>本服务无 MyBatis 用法，但 common 拉了 mybatis-spring → 排除 {@link DataSourceAutoConfiguration}
 * 避免 Spring Boot 尝试建 DataSource。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.railway.search.feign")
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
