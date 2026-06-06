package com.railway.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * ES 检索服务入口。
 *
 * <p>职责：
 * <ul>
 *   <li>{@code /search/trains}：车次条件检索（ES）</li>
 *   <li>{@code /search/trains/{trainNo}/seats}：座位余票（Feign → service-train-stock）</li>
 * </ul>
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.railway.search.feign")
@EnableElasticsearchRepositories(basePackages = "com.railway.search.repository")
@SpringBootApplication
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
